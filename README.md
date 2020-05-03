# java-refactoring

[![Build Status](https://travis-ci.com/TQRG/leafactor-ci.svg?token=35rpGpzubsgs2UqfNV5N&branch=master)](https://travis-ci.com/TQRG/leafactor-ci)
[![Maven metadata URL](https://img.shields.io/maven-metadata/v?label=Plugin&metadataUrl=https://plugins.gradle.org/m2/tqrg/leafactor/ci/tqrg.leafactor.ci.gradle.plugin/maven-metadata.xml)](https://plugins.gradle.org/plugin/de.inetsoftware.jwebassembly)
![Maven metadata URL](https://img.shields.io/badge/Latest%20Release-Alpha-blue)

LeafactorCI is a gradle plugin for refactoring battery inefficient anti-patterns in Android applications.
It works by scanning the sources files of the Android project in order to find and refactor battery inefficient anti-patterns automatically. 
LeafactorCI is able to integrate a CI pipeline, by refactoring the source code and consequently committing the changes to your GIT repository.

Currently LeafactorCI is in Alpha and can refactor the following patterns:
- Recycle
- ViewHolder 
- DrawAllocation
- WakeLock

These patterns were shown to affect battery efficiency, see more in
[Performance-based Guidelines for Energy Efficient Mobile Applications](https://luiscruz.github.io/papers/cruz2017performance.pdf).

# Installation 

In your android project go to the app/build.gradle and add the following:
```
plugins {
  id 'com.android.application'
  id "tqrg.leafactor.ci" version "PLUGIN_VERSION"
}
```

Where PLUGIN_VERSION is the version of the plugin you would like to use.
Make sure that com.android.application plugin is applied first.

# Usage

Refactoring the app is very simples, simply run the following command.
```
gradlew app:refactor
```

Note: LeafactorCI is still in Alpha, there are still bugs to iron out.
When refactoring your App with LeafactorCI make sure that you can revert its changes either by committing your files or changing your branch.

## Adding continuous integration

To add continuous integration with a Leafactor CI stage where a commit is generated and sent to the git repository in a new branch, add the following code to your CI pipeline:
```bash
git config user.email "EMAIL_OF_THE_COMMIT_AUTHOR"
git config user.name "NAME_OF_THE_COMMIT_AUTHOR"
REV=$(git rev-parse --short HEAD)
git checkout -b "leafactor-refactoring-$REV"
./gradlew build
./gradlew refactor
cd app/src
git add .
cd ../../
git commit --allow-empty -m "LeafactorCI refactoring changes."
git remote rm origin
git remote add origin "GIT_REPOSITORY_URL"
git push origin "leafactor-refactoring-$REV"
```

## Options

Will be available soon.


# Contributing

LeafactorCI is base on [INRIA/spoon](https://github.com/INRIA/spoon). 

## Requirements

Java 8 JDK - Make sure you have the Java 8 JDK and its referenced in the JAVA_HOME environment variable.

## Tests

To run tests use:
```
gradlew :cleanTest :test --tests "tqrg.leafactor-ci.cli.rules.TestRules"
```

## Publishing
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

## Known Bugs
- LeafactorCI is importing the same package:
    https://github.com/INRIA/spoon/issues/3267
- Tests are not passing:
    This is due to the Spoon issue https://github.com/INRIA/spoon/issues/3267
- LeafactorCI recycling an unrecyclable variable: Needs to be addressed
- LeafactorCI adding variables that already exist: Needs to be addressed
- LeafactorCI adding fully qualified names: Needs to be addressed, is an issue with Spoon

## Future
- Add support for classpath integration (Partly done but not ready)

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

## LeafactorCI is importing the same package

This is an ongoing issue in spoon, more information can be seen on:
https://github.com/INRIA/spoon/issues/3267

## I found a problem with LeafactorCI

Please file a new issue. Resolve time will depend on LeafactorCI maintainer availability as well as INRIA/Spoon maintainer availability.