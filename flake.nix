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
          pkgs.libx11
          pkgs.libxcursor
          pkgs.libxrandr
          pkgs.libxi
          pkgs.libxxf86vm
          pkgs.libxext
          pkgs.libxrender
          pkgs.libxtst
          pkgs.vulkan-loader
        ];
      };
    };
}
