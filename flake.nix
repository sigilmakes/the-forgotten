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
          pkgs.temurin-jre-bin-21
          pkgs.temurin-bin-21
        ];

        JAVA_HOME = pkgs.temurin-bin-21;
      };
    };
}
