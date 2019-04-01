package com.leafactor.cli.engine;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.printer.YamlPrinter;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import com.leafactor.cli.utility.Color;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A group of compilation units defined by a directory path
 */
public class CompilationUnitGroup {
    private List<File> files;

    /**
     * Creates a compilation unit group
     */
    public CompilationUnitGroup() {
        files = new ArrayList<>();
    }

    public void add(File file) throws IOException {
        if(file.isDirectory()) {
            files.addAll(getDirectoryFiles(file));
        } else {
            files.add(file);
        }
    }

    /**
     * Returns the Java files in the directory as alist
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
     * @param file Java file provided (.java)
     * @param refactoringRules List of refactoring rules
     * @return The entire refactored result file content  as a string
     * @throws FileNotFoundException Thrown when the file is not found
     */
    private String runFile(File file, List<RefactoringRule> refactoringRules) throws FileNotFoundException {
        System.out.println("FILE:" + file.getName());
        FileInputStream in = new FileInputStream(file);
        JavaParser javaParser = new JavaParser();
        CompilationUnit cuBefore = javaParser.parse(in);
        CompilationUnit cu = LexicalPreservingPrinter.setup(cuBefore);
        for(RefactoringRule rule : refactoringRules) {
            rule.apply(cu);
        }
        return LexicalPreservingPrinter.print(cu);
    }

    /**
     * Persists a set of files with the corresponding results (string) through a map
     * @param results A Map where each key is a java File and a string is the content
     * @throws FileNotFoundException Thrown when a file is not found
     */
    private void persist(Map<File, String> results) throws FileNotFoundException {
        for(File file : results.keySet()) {
            PrintWriter pw = new PrintWriter(file);
            pw.println(results.get(file));
            pw.close();
        }
    }

    /**
     * Executes a refactoring job with a list of refactoring rules over the directory
     * @param refactoringRules A list of refactoring rules
     * @throws IOException Thrown when there is IO exception
     */
    public void run(List<RefactoringRule> refactoringRules) throws IOException {
        if(refactoringRules == null) {
            throw new RuntimeException("The refactoring rules list cannot be null");
        }
        if(refactoringRules.size() == 0) {
            throw new RuntimeException("The refactoring rules list cannot be empty");
        }

        // We save the result, we want to persist only if every file is successfully refactored
        Map<File, String> results = new HashMap<>();
        for(File file : this.files) {
            results.put(file, this.runFile(file, refactoringRules));
        }
        // Lets persist all the files that we changed
        this.persist(results);
    }

    /**
     * Prints the Abstract Syntax Tree as a .yml file for every Java file in the directory
     * @throws IOException Thrown when there is IO exception
     */
    public void printYaml() throws IOException {
        System.out.println("PRINTING YAML FILES");
        YamlPrinter printer = new YamlPrinter(true);
        // We save the result, we want to persist only if every file is successfully refactored
        Map<File, String> results = new HashMap<>();
        for(File file : this.files) {
            FileInputStream in = new FileInputStream(file);
            JavaParser javaParser = new JavaParser();
            CompilationUnit cu = LexicalPreservingPrinter.setup(javaParser.parse(in));
            String outputContent = printer.output(cu);
            System.out.println("File: " + file.getName());
            System.out.println(Color.CYAN);
            System.out.println(outputContent);
            System.out.println(Color.RESET);
            File output = new File(file.getParent() + "/" +
                    file.getName().substring(0, file.getName().lastIndexOf(".java")) + ".yml");
            results.put(output, outputContent);
        }
        this.persist(results);
    }
}
