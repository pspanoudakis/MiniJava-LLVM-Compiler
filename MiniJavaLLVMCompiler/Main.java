/**
 * File: Main.java
 * Pavlos Spanoudakis (sdi1800184)
 */

import syntaxtree.*;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import Visitors.*;

public class Main {
    public static void main(String[] args) throws Exception {
        if(args.length < 1) {
            System.err.println("No file path arguments given.");
            System.err.println("Usage: java Main <inputFile1> <restInputFiles>*");
            System.exit(1);
        }
        
        /**
         * For each given file, perform Semantic Analysis.
         * If no errors are detected, generate the corresponding LLVM IR file.
         */
        for (String filepath: args)
        {
            FileInputStream fis = null;
            System.out.println();
            try {
                // Open file and create Parse Tree
                fis = new FileInputStream(filepath);
                System.out.println("File: " + filepath);
                System.out.println("-----------------------------------");

                MiniJavaParser parser = new MiniJavaParser(fis);    
                Goal root = parser.Goal();    
                System.err.println("Program parsed successfully.");
    
                /* ------------------- Semantic Analysis ------------------- */
                
                // Collect all class names
                ClassNameCollector classNameCollector = new ClassNameCollector();
                root.accept(classNameCollector, null);

                // Collect all class fields and methods
                DeclarationCollector declarationCollector = new DeclarationCollector(classNameCollector.classInfos);
                root.accept(declarationCollector, null);

                // Check method bodies
                FunctionBodyAnalyzer functionAnalyzer = new FunctionBodyAnalyzer(classNameCollector.classInfos);
                root.accept(functionAnalyzer, null);

                // TODO: Not to be included
                System.out.println("Semantic Analysis has been completed.");
                functionAnalyzer.printOffsets();

                /* ------------------- LLVM IR Generation ------------------ */
                
                // Create output .ll file
                String outputFile = filepath.replace(".java", ".ll");
                IRGenerator irgen = new IRGenerator(outputFile, classNameCollector.classInfos);
                irgen.emitVtables();
                irgen.emitUtils();
                root.accept(irgen, null);
                irgen.closeWriter();

                // Done.
                System.out.println("LLVM IR file: '" + outputFile + "' has been produced.");
            }
            catch(ParseException ex){
                // Parsing failed
                System.out.println(ex.getMessage());
            }
            catch(FileNotFoundException ex){
                // Invalid file path given
                System.err.println(ex.getMessage());
            }
            catch(SemanticError er){
                // Semantic Error detected
                System.err.println(er.getMessage());
            }
            finally {
                try {
                    if(fis != null) fis.close();
                }
                catch(IOException ex) {
                    System.err.println(ex.getMessage());
                }
            }
        }
    }
}
