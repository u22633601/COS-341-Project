import java.io.*;
import java.util.Scanner;

public class Compiler {

    // ANSI escape codes for color formatting
    public static final String RESET = "\u001B[0m";
    public static final String GREEN = "\u001B[32m";
    public static final String RED = "\u001B[31m";
    public static final String CYAN = "\u001B[36m";      // Lexer
    public static final String YELLOW = "\u001B[33m";    // Parser
    public static final String BLUE = "\u001B[34m";       // Scope Analyzer
    public static final String MAGENTA = "\u001B[35m";    // Type Checker
    public static final String LIGHT_GREEN = "\u001B[92m"; // Intermediate Code Generator
    public static final String LIGHT_CYAN = "\u001B[96m";  // Target Code Generator

    public static void main(String[] args) throws IOException, InterruptedException {
        Scanner scanner = new Scanner(System.in);

        // Ask the user for the input file name and target .bas file
        System.out.print("Enter the name of the input file (e.g., input.txt): ");
        String inputFile = scanner.nextLine();
        
        System.out.print("Enter the name of the target .bas file (e.g., tc.bas): ");
        String targetFile = scanner.nextLine();

        // 1. Run Lexer
        System.out.println(CYAN + "Running Lexer..." + RESET);
        //System.out.println(CYAN + "Executing: java Main " + inputFile + RESET);
        if (runCommand("java Main " + inputFile, "Tokenization complete", "Invalid token encountered")) {
            System.out.println(GREEN + "Lexer completed successfully. Output written to output.xml." + RESET);
            System.out.println("Do you want to continue with Parser? (y/n)");
            if (scanner.nextLine().equalsIgnoreCase("y")) {

                // 2. Run Parser
                System.out.println(YELLOW + "Running Parser..." + RESET);
                //System.out.println(YELLOW + "Executing: java SLRParser output.xml parser.xml" + RESET);
                if (runCommand("java SLRParser output.xml parser.xml", "Parsing completed successfully", "Error during parsing")) {
                    System.out.println(GREEN + "Parser completed successfully. Output written to parser.xml." + RESET);
                    System.out.println("Do you want to continue with Scope Analyzer? (y/n)");

                    if (scanner.nextLine().equalsIgnoreCase("y")) {

                        // 3. Run Scope Analyzer
                        System.out.println(BLUE + "Running Scope Analyzer..." + RESET);
                        //System.out.println(BLUE + "Executing: java DualScopeAnalyzer" + RESET);
                        if (runCommand("java DualScopeAnalyzer", "Symbol table has been written to Symbol.txt", "ScopeAnalyzer encountered an error")) {
                            System.out.println(GREEN + "Scope Analyzer completed successfully." + RESET);
                            System.out.println("Do you want to continue with Type Checker? (y/n)");

                            if (scanner.nextLine().equalsIgnoreCase("y")) {

                                // 4. Run Type Checker
                                System.out.println(MAGENTA + "Running Type Checker..." + RESET);
                                //System.out.println(MAGENTA + "Executing: java TypeChecker " + inputFile + RESET);
                                if (runCommand("java TypeChecker " + inputFile, "No type errors found", "TypeChecker encountered an error")) {
                                    System.out.println(GREEN + "Type Checker completed successfully. No type errors found." + RESET);
                                    System.out.println("Do you want to continue with Intermediate Code Generator? (y/n)");

                                    if (scanner.nextLine().equalsIgnoreCase("y")) {

                                        // 5. Run Intermediate Code Generator
                                        System.out.println(LIGHT_GREEN + "Running Intermediate Code Generator..." + RESET);
                                        //System.out.println(LIGHT_GREEN + "Executing: java DirectCodeGenerator " + inputFile + RESET);
                                        if (runCommand("java DirectCodeGenerator " + inputFile, "Generated Code", "Error during code generation")) {
                                            System.out.println(GREEN + "Intermediate Code Generator completed successfully." + RESET);
                                            System.out.println("Do you want to continue with Target Code Generator? (y/n)");

                                            if (scanner.nextLine().equalsIgnoreCase("y")) {

                                                // 6. Run Target Code Generator
                                                System.out.println(LIGHT_CYAN + "Running Target Code Generator..." + RESET);
                                                //System.out.println(LIGHT_CYAN + "Executing: java TargetCode " + targetFile + RESET);
                                                if (runCommand("java TargetCode " + targetFile, "BASIC code has been written to " + targetFile, "Error during target code generation")) {
                                                    System.out.println(GREEN + "Target Code Generation completed successfully." + RESET);
                                                } else {
                                                    System.err.println(RED + "Error running Target Code Generator. Please check the logs." + RESET);
                                                }

                                            }
                                        } else {
                                            System.err.println(RED + "Error running Intermediate Code Generator. Please check the logs." + RESET);
                                        }
                                    }
                                } else {
                                    System.err.println(RED + "Error running Type Checker. Please check the logs." + RESET);
                                }
                            }
                        } else {
                            System.err.println(RED + "Error running Scope Analyzer. Please check the logs." + RESET);
                        }
                    }
                } else {
                    System.err.println(RED + "Error running Parser. Please check the logs." + RESET);
                }
            }
        } else {
            System.err.println(RED + "Error running Lexer. Please check the logs." + RESET);
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
    
        // Print standard output
        while ((line = reader.readLine()) != null) {
            System.out.println(line);
            if (line.contains(successMessage)) {
                success = true;
            }
        }
    
        // Check process exit code
        int exitCode = process.waitFor();
    
        // If the process exited successfully, don't check error output
        if (exitCode != 0) {
            while ((line = errorReader.readLine()) != null) {
                System.err.println(line);
                if (line.contains(errorMessage)) {
                    success = false;
                }
            }
        }
    
        return success && exitCode == 0;
    }
}
