package spl.lae;
import java.io.IOException;

import parser.*;
import scheduling.TiredThread;

public class Main {
    public static void main(String[] args) {
        // TODO: main
        //input->input parser -> computationNode -> engine.run(computationNode) ->
        // solves by using threads on each operator recursively -> solutionNode ->outputWriter

        if (args.length != 3) {
            System.err.println("Error: Invalid number of arguments.");
            System.exit(1);
        }
        try {
            int numThreads = Integer.parseInt(args[0]);
            String inputPath = args[1];
            String outputPath = args[2];
            if(numThreads<1){
                System.err.println("Error: Number of threads must be at least 1.");
                return;
            }
            try {
                InputParser inputParser = new InputParser();
                ComputationNode computationRoot = inputParser.parse(inputPath);
                LinearAlgebraEngine engine = new LinearAlgebraEngine(numThreads);
                ComputationNode solutionNode = engine.run(computationRoot);
                double[][] solutionMatrix = solutionNode.getMatrix();
                OutputWriter.write(solutionMatrix, outputPath);

            } catch (Exception e) {
                OutputWriter.write(e.getMessage(), outputPath);
            }


        } catch (NumberFormatException e) {
            System.err.println("Error: The first argument (numThreads) must be an integer.");
            System.exit(1);
        } catch (Exception e) {
            System.err.println("An unexpected error occurred: " + e.getMessage());
        }
    }
}