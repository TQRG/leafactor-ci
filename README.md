# java-refactoring

[![Build Status](https://travis-ci.com/TQRG/leafactor-ci.svg?token=35rpGpzubsgs2UqfNV5N&branch=master)](https://travis-ci.com/TQRG/leafactor-ci)
[![Maven metadata URL](https://img.shields.io/maven-metadata/v?label=Plugin&metadataUrl=https://plugins.gradle.org/m2/tqrg/leafactor/ci/tqrg.leafactor.ci.gradle.plugin/maven-metadata.xml)](https://plugins.gradle.org/plugin/de.inetsoftware.jwebassembly)
![Maven metadata URL](https://img.shields.io/badge/Latest%20Release-Alpha-blue)

# Requirements

Java 8 JDK - Make sure you have the Java 8 JDK and its referenced in the JAVA_HOME environment variable.

# Tests

To run tests use:
```
gradlew :cleanTest :test --tests "tqrg.leafactor-ci.cli.rules.TestRules"
```

Note: Tests in IntelliJ look unorganized because gradle currently has no support for dynamic test names, [see more.](https://github.com/gradle/gradle/issues/5975) 

# Publishing
The publishing version can be found in the gradle.properties file. A version can only be published once.
To publish a version we must have a previously initialized login session to the gradle plugin registry, to sign in use:

```
gradlew login
```

After authorization is granted, we can proceed to the publishing of the version.

```
gradlew publishPlugins
``` 

Note:
If the task fails due to javadoc problems, execute the following command and try again:
```
gradlew javadoc
```


# Installation 

Make sure that com.android.application plugin is applied first.



# FAQ

## How to use jar instead of the published version:

First clone the Spoon repository then go to the folder and generate a jar with dependencies:
```bash
git clone https://github.com/INRIA/spoon.git
cd spoon
mvn clean compile assembly:single
```

The generated file should be under the target folder. e.g. spoon/target/spoon-core-VERSION-SNAPSHOT-jar-with-dependencies.jar

Then generate the jar for leafactor-ci:

```bash
cd PATH_TO_LEAFACTOR_REPOSITORY
gradlew :jar
```

Now in the android project, add the following properties to the local.properties file:

```
leafactor.jar=PATH_TO_LEAFACTOR_JAR
spoon-core.jar=PATH_TO_SPOON_JAR
```

Now we need to add both jar files to the class path of the app and add the leafactor plugin.
For mostly every project you will find the build.gradle file inside ./app folder of the android project 
and add following instruction to the top of the file:

```
buildscript {
    repositories {
        google()
    }
    dependencies {
        Properties properties = new Properties()
        properties.load(project.rootProject.file('local.properties').newDataInputStream())
        def leafactorDir = properties.getProperty('leafactor.jar')
        def spoonCoreDir = properties.getProperty('spoon-core.jar')
        classpath files(leafactorDir)
        classpath files(spoonCoreDir)
    }
}

plugins {
    id 'com.android.application'
}

apply plugin: tqrg.leafactor.ci.gradle.plugin.LeafactorCIPlugin
``` 