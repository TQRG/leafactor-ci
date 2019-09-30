package com.leafactor.gradle.plugin;


import org.gradle.api.file.FileCollection;
import org.gradle.api.internal.file.UnionFileCollection;

public class LauncherExtension  {
    private FileCollection files = new UnionFileCollection();

    public FileCollection getFiles() {
        return files;
    }

    public void setFiles(FileCollection files) {
        this.files = files;
    }
}