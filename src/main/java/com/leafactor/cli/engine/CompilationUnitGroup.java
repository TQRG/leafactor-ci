package com.leafactor.cli.engine;

import spoon.Launcher;
import spoon.compiler.Environment;
import spoon.reflect.CtModel;
import spoon.support.sniper.SniperJavaPrettyPrinter;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Represents a group of compilation units defined by a list of files
 */
public class CompilationUnitGroup {

    private List<File> files;

    /**
     * Creates a compilation unit group
     */
    public CompilationUnitGroup() {
        files = new ArrayList<>();
    }

    /**
     * Adds a new file to the list of files that make up the compilation unit group
     *
     * @param file The file to add
     * @throws IOException Thrown when there is IO exception
     */
    public void add(File file) throws IOException {
        if (file.isDirectory()) {
            files.addAll(getDirectoryFiles(file));
        } else {
            files.add(file);
        }
    }

    /**
     * Returns the Java files in the directory as alist
     *
     * @return A list of Java files
     * @throws IOException Thrown when there is IO exception
     */
    private static List<File> getDirectoryFiles(File directory) throws IOException {
        List<File> files = new ArrayList<>();
        Files.find(Paths.get(directory.getPath()),
                Integer.MAX_VALUE,
                (filePath, fileAttr) -> fileAttr.isRegularFile())
                .forEach(path -> files.add(path.toFile()));
        return files.stream().filter(file -> file.getName().endsWith(".java")).collect(Collectors.toList());
    }

    /**
     * Executes a refactoring job for a list of refacoring rules over a given .java file
     *
     * @param file             Java file provided (.java)
     * @param refactoringRules List of refactoring rules
     * @return The entire refactored result file content  as a string
     * @throws FileNotFoundException Thrown when the file is not found
     */
    private String runFile(File file, List<RefactoringRule> refactoringRules) throws IOException {
        System.out.println("FILE:" + file.getName());
//        FileInputStream in = new FileInputStream(file);
        final Launcher launcher = new Launcher();
        final Environment e = launcher.getEnvironment();
//        e.setLevel("INFO");
        e.setNoClasspath(true);
        e.setAutoImports(true);
        launcher.getEnvironment().setPrettyPrinterCreator(() -> new SniperJavaPrettyPrinter(launcher.getEnvironment()));
        launcher.addInputResource(file.getAbsolutePath());
        for (RefactoringRule rule : refactoringRules) {
            launcher.addProcessor(rule);
        }
        Path tempDir = Files.createTempDirectory("temporary-output");
        launcher.setSourceOutputDirectory(tempDir.toFile());
        try {
            launcher.run();
        } catch(Exception exception) {
            System.out.println("Could not read file " + file + "`\nSkipping it...");
            return null;
        }
        CtModel model = launcher.getModel();
        String packageName = model.getAllPackages().toArray()[model.getAllPackages().size() - 1].toString();
        packageName = packageName.replaceAll("\\.", "/");

        return new String(Files.readAllBytes(Paths.get(tempDir + "/" + packageName + "/" + file.getName())));
    }

    /**
     * Persists a set of files with the corresponding results (string) through a map
     *
     * @param results A Map where each key is a java File and a string is the content
     * @throws FileNotFoundException Thrown when a file is not found
     */
    private void persist(Map<File, String> results) throws FileNotFoundException {
        for (File file : results.keySet()) {
            PrintWriter pw = new PrintWriter(file);
            pw.println(results.get(file));
            pw.close();
        }
    }

    /**
     * Executes a refactoring job with a list of refactoring rules over the directory
     *
     * @param refactoringRules A list of refactoring rules
     * @throws IOException Thrown when there is IO exception
     */
    public void run(List<RefactoringRule> refactoringRules) throws IOException {
        if (refactoringRules == null) {
            throw new RuntimeException("The refactoring rules list cannot be null");
        }
        if (refactoringRules.size() == 0) {
            throw new RuntimeException("The refactoring rules list cannot be empty");
        }

        // We save the result, we want to persist only if every file is successfully refactored
        Map<File, String> results = new HashMap<>();
        for (File file : this.files) {
            String result = this.runFile(file, refactoringRules);
            if(result != null) {
                results.put(file, result);
            }

        }
        // Lets persist all the files that we changed
        this.persist(results);
    }
}
