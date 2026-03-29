///usr/bin/env java --source 21 "$0" "$@"; exit $?
// Usage: java hotswap.java <classes-dir>
// Connects to JDWP on port 5005 and redefines all .class files found under <classes-dir>

import com.sun.jdi.*;
import com.sun.jdi.connect.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class hotswap {
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: java hotswap.java <classes-dir> [--changed-since <seconds>]");
            System.exit(1);
        }

        Path classesDir = Path.of(args[0]);
        long changedSince = 0;
        if (args.length >= 3 && args[1].equals("--changed-since")) {
            changedSince = System.currentTimeMillis() - (Long.parseLong(args[2]) * 1000);
        }

        // Find all .class files (optionally filtered by mtime)
        List<Path> classFiles = new ArrayList<>();
        final long since = changedSince;
        Files.walk(classesDir)
            .filter(p -> p.toString().endsWith(".class"))
            .filter(p -> {
                if (since == 0) return true;
                try { return Files.getLastModifiedTime(p).toMillis() >= since; }
                catch (IOException e) { return false; }
            })
            .forEach(classFiles::add);

        if (classFiles.isEmpty()) {
            System.out.println("No changed classes found.");
            return;
        }

        // Connect to JDWP
        AttachingConnector connector = Bootstrap.virtualMachineManager()
            .attachingConnectors().stream()
            .filter(c -> c.transport().name().equals("dt_socket"))
            .findFirst().orElseThrow(() -> new RuntimeException("No socket connector"));

        Map<String, Connector.Argument> connArgs = connector.defaultArguments();
        connArgs.get("hostname").setValue("localhost");
        connArgs.get("port").setValue("5005");

        VirtualMachine vm;
        try {
            vm = connector.attach(connArgs);
        } catch (Exception e) {
            System.err.println("Failed to connect to JDWP on port 5005. Is Minecraft running with debug?");
            System.exit(1);
            return;
        }

        System.out.printf("Connected to JVM. Redefining %d class(es)...%n", classFiles.size());

        // Build redefinition map
        Map<ReferenceType, byte[]> redefs = new HashMap<>();
        int skipped = 0;

        for (Path classFile : classFiles) {
            // Convert path to class name: com/example/Foo.class -> com.example.Foo
            String relative = classesDir.relativize(classFile).toString();
            String className = relative.replace(File.separatorChar, '.').replace(".class", "");

            List<ReferenceType> types = vm.classesByName(className);
            if (types.isEmpty()) {
                skipped++;
                continue;
            }

            byte[] bytecode = Files.readAllBytes(classFile);
            redefs.put(types.get(0), bytecode);
        }

        if (redefs.isEmpty()) {
            System.out.printf("No loaded classes matched (%d skipped — not yet loaded).%n", skipped);
            vm.dispose();
            return;
        }

        try {
            vm.redefineClasses(redefs);
            System.out.printf("✓ Redefined %d class(es)", redefs.size());
            if (skipped > 0) System.out.printf(" (%d skipped)", skipped);
            System.out.println();
            redefs.keySet().forEach(t -> System.out.println("  " + t.name()));
        } catch (Exception e) {
            System.err.println("✗ Redefinition failed: " + e.getMessage());
            System.err.println("  (Mixin or structural change? Restart needed.)");
        }

        vm.dispose();
    }
}
