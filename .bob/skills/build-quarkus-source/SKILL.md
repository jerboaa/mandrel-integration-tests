---
name: build-quarkus-source
description: >-
  Builds Quarkus from source. Useful to verify quarkus issues in
  mandrel-integration-tests.
---

# Build Quarkus `main` from source

1. Clone the quarkus git repo at https://github.com/quarkusio/quarkus into folder ~/quarkus-source, check out the `main` branch. Make sure to not re-clone the repository when it's already cloned.
2. Download a JDK 21 JDK for the current platform using the Temurin API URL (for Linux x64) `https://api.adoptium.net/v3/binary/latest/21/ga/linux/x64/jdk/hotspot/normal/eclipse`. Extract the tarball into ~/jdk-21 if it doesn't yet exist.
3. Finally, go to directory ~/quarkus-source and build quarkus by using the following command `JAVA_HOME=~/jdk-21 ./mvnw clean install -Dquickly`
