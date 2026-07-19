{
  description = "Kotlin Gradle Project";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = { self, nixpkgs, flake-utils, ... }:
    flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = nixpkgs.legacyPackages.${system};

        # Specify the JDK version you need (e.g., jdk17, jdk21)
        jdk = pkgs.jdk21;

        # Ensure Gradle uses the exact JDK we specified above
        gradle = pkgs.gradle.override { java = jdk; };

        # X11/GTK libraries required by JavaFX native rendering
        javafxLibs = with pkgs; [
          libx11
          libxext
          libxrender
          libxtst
          libxi
          libxxf86vm
          libGL
          glib
          gtk3
          freetype
          fontconfig
          zlib
        ];

        nativeBuildInputs = with pkgs; [
          jdk
          gradle
          kotlin
          kotlin-language-server # For IDE support
          ktlint                 # For linting/formatting

          # Kept from your original setup if you still need them:
          nodejs_22
          docker-compose
          usql
        ];

        buildInputs = javafxLibs;
      in {
        devShells.default = pkgs.mkShell {
          inherit nativeBuildInputs buildInputs;

          # Gradle and other JVM tools rely heavily on JAVA_HOME
          shellHook = ''
            export JAVA_HOME=${jdk.home}
            export LD_LIBRARY_PATH=${pkgs.lib.makeLibraryPath javafxLibs}:$LD_LIBRARY_PATH
          '';
        };

        # Note on Nix builds:
        # I removed the `packages.default` block because building a Gradle 
        # project inside a pure Nix derivation is completely different from Go.
        # `buildGoModule` easily handles Go's vendor hashing, but Gradle's 
        # dynamic dependency resolution requires network access. 
        # 
        # To make `nix build` work for Gradle, you typically need a two-step 
        # fixed-output derivation (to pre-fetch dependencies) or a tool like `gradle2nix`. 
        # For standard local development, just using `nix develop` (the devShell above) 
        # and running `./gradlew build` is exactly what you need.
      });
}
