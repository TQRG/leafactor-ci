package com.leafactor.gradle.plugin;


import org.gradle.api.file.FileCollection;
import org.gradle.api.internal.file.UnionFileCollection;

public class LauncherExtension  {
    private FileCollection classPath = new UnionFileCollection();

    public FileCollection getClassPath() {
        return classPath;
    }

    public void setClassPath(FileCollection classPath) {
        this.classPath = classPath;
    }
}