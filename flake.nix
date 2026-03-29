{
  description = "The Forgotten — Minecraft Fabric mod";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
  };

  outputs = { self, nixpkgs }:
    let
      system = "x86_64-linux";
      pkgs = nixpkgs.legacyPackages.${system};

    in {
      devShells.${system}.default = pkgs.mkShell {
        packages = [
          pkgs.jetbrains.jdk
          pkgs.gradle
        ];

        JAVA_HOME = pkgs.jetbrains.jdk;

        LD_LIBRARY_PATH = pkgs.lib.makeLibraryPath [
          pkgs.libGL
          pkgs.glfw
          pkgs.openal
          pkgs.flite
          pkgs.libpulseaudio
          pkgs.udev
          pkgs.xorg.libX11
          pkgs.xorg.libXcursor
          pkgs.xorg.libXrandr
          pkgs.xorg.libXi
          pkgs.xorg.libXxf86vm
          pkgs.xorg.libXext
          pkgs.xorg.libXrender
          pkgs.xorg.libXtst
          pkgs.vulkan-loader
        ];
      };
    };
}
