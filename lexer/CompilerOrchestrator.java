import java.io.*;
import java.util.Scanner;

public class CompilerOrchestrator {

    public static void main(String[] args) throws IOException, InterruptedException {
        Scanner scanner = new Scanner(System.in);

        // 1. Run Lexer
        System.out.println("Running Lexer...");
        if (runCommand("java Main input.txt", "Tokenization complete", "Invalid token encountered")) {
            System.out.println("Lexer completed successfully. Output written to output.xml.");
            System.out.println("Do you want to continue with Parser? (y/n)");
            if (scanner.nextLine().equalsIgnoreCase("y")) {

                // 2. Run Parser
                System.out.println("Running Parser...");
                if (runCommand("java SLRParser output.xml parser.xml", "Parsing completed successfully", "Error during parsing")) {
                    System.out.println("Parser completed successfully. Output written to parser.xml.");
                    System.out.println("Do you want to continue with Scope Analyzer? (y/n)");

                    if (scanner.nextLine().equalsIgnoreCase("y")) {

                        // 3. Run Scope Analyzer
                        System.out.println("Running Scope Analyzer...");
                        if (runCommand("java DualScopeAnalyzer", "Symbol table has been written to Symbol.txt", "ScopeAnalyzer encountered an error")) {
                            System.out.println("Scope Analyzer completed successfully.");
                            System.out.println("Do you want to continue with Type Checker? (y/n)");

                            if (scanner.nextLine().equalsIgnoreCase("y")) {

                                // 4. Run Type Checker
                                System.out.println("Running Type Checker...");
                                if (runCommand("java TypeChecker", "No type errors found", "TypeChecker encountered an error")) {
                                    System.out.println("Type Checker completed successfully. No type errors found.");
                                    System.out.println("Do you want to continue with Intermediate Code Generator? (y/n)");

                                    if (scanner.nextLine().equalsIgnoreCase("y")) {

                                        // 5. Run Intermediate Code Generator
                                        System.out.println("Running Intermediate Code Generator...");
                                        if (runCommand("java DirectCodeGenerator", "Generated Code", "Error during code generation")) {
                                            System.out.println("Intermediate Code Generator completed successfully.");
                                            System.out.println("Do you want to continue with Target Code Generator? (y/n)");

                                            if (scanner.nextLine().equalsIgnoreCase("y")) {

                                                // 6. Run Target Code Generator
                                                System.out.println("Running Target Code Generator...");
                                                if (runCommand("java TargetCode", "END", "Error during target code generation")) {
                                                    System.out.println("Target Code Generation completed successfully.");
                                                } else {
                                                    System.err.println("Error running Target Code Generator. Please check the logs.");
                                                }
                                            }
                                        } else {
                                            System.err.println("Error running Intermediate Code Generator. Please check the logs.");
                                        }
                                    }
                                } else {
                                    System.err.println("Error running Type Checker. Please check the logs.");
                                }
                            }
                        } else {
                            System.err.println("Error running Scope Analyzer. Please check the logs.");
                        }
                    }
                } else {
                    System.err.println("Error running Parser. Please check the logs.");
                }
            }
        } else {
            System.err.println("Error running Lexer. Please check the logs.");
        }

        scanner.close();
    }

    public static boolean runCommand(String command, String successMessage, String errorMessage) throws IOException, InterruptedException {
        // Split the command into parts for ProcessBuilder
        String[] commandParts = command.split(" ");
        ProcessBuilder processBuilder = new ProcessBuilder(commandParts);
        Process process = processBuilder.start();
    
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        String line;
        boolean success = false;
    
        // Check for success or error messages in the output
        while ((line = reader.readLine()) != null) {
            System.out.println(line);
            if (line.contains(successMessage)) {
                success = true;
            }
        }
    
        // Check if any error occurred
        while ((line = errorReader.readLine()) != null) {
            System.err.println(line);
            if (line.contains(errorMessage)) {
                success = false;
            }
        }
    
        process.waitFor();
        return success && process.exitValue() == 0;
    }
    
}
