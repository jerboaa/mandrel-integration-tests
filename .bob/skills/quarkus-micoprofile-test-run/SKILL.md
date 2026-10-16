---
name: quarkus-micoprofile-test-run
description: Run the quarkus microprofile mandrel integration test
---

# Run Quarkus microprofile mandrel integration test

- First ask for the quarkus version that should be used to test, defaulting to `999-SNAPSHOT` for quarkus main branch.
- Verify that if quarkus main has been specified, that the local build of quarkus main is no older than 2 days. Use skill quarkus main from source to build it from source otherwise. Look at the quarkus-bom artefact in the locally installed maven repository ~/.m2/repository. The file's last modified time.
- Set JAVA_HOME to ~/jdk-21. Verify that it exists.
- Ask for the GraalVM build to use, default to ~/mandrel-build, Set GRAALVM_HOME bash environment variable to that location. Also set the bash environment variable PATH like so: PATH=$GRAALVM_HOME/bin:$PATH
- Ensure that Apache Maven is installed, Download it and install to ~/maven otherwise.
- Run the quarkus microprofile native integration test using this command `mvn clean verify  -Dquarkus.version=999-SNAPSHOT -Ptestsuite -Dtest=RuntimesSmokeTest#quarkusFullMicroProfile`. The `quarkus.version` property value should be the one determined in step 1
