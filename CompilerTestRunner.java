import java.io.*;
import java.nio.file.*;
import java.util.*;

public class CompilerTestRunner {
    private static final String TEST_RESOURCES_DIR = "test_resources/";
    private static final String TEST_OUTPUT_DIR = "test_output/";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_BLUE = "\u001B[34m";
    private static final String ANSI_RESET = "\u001B[0m";

    static class TestCase {
        String name;
        String inputContent;
        boolean expectedLexerSuccess;
        boolean expectedParserSuccess;
        boolean expectedScopeSuccess;
        boolean expectedTypeSuccess;
        List<String> expectedLexerErrors;
        List<String> expectedParserErrors;
        List<String> expectedScopeErrors;
        List<String> expectedTypeErrors;

        public TestCase(String name, String inputContent) {
            this.name = name;
            this.inputContent = inputContent;
            this.expectedLexerSuccess = true;
            this.expectedParserSuccess = true;
            this.expectedScopeSuccess = true;
            this.expectedTypeSuccess = true;
            this.expectedLexerErrors = new ArrayList<>();
            this.expectedParserErrors = new ArrayList<>();
            this.expectedScopeErrors = new ArrayList<>();
            this.expectedTypeErrors = new ArrayList<>();
        }
    }

    private static class TestResult {
        boolean passed;
        String message;

        TestResult(boolean passed, String message) {
            this.passed = passed;
            this.message = message;
        }
    }

    public static void main(String[] args) {
        createDirectories();
        List<TestCase> testCases = createTestCases();

        int totalTests = testCases.size();
        int passedTests = 0;

        System.out.println(ANSI_BLUE + "\n=== Compiler Test Suite ===" + ANSI_RESET);
        System.out.println("Total test cases: " + totalTests);
        System.out.println("\nRunning tests...\n");

        for (TestCase test : testCases) {
            boolean testPassed = runTest(test);
            if (testPassed)
                passedTests++;
            System.out.println("-".repeat(50));
        }

        printSummary(totalTests, passedTests);
    }

    private static void printSummary(int totalTests, int passedTests) {
        System.out.println("\n=== Test Summary ===");
        System.out.println("Total Tests: " + totalTests);
        System.out.println("Passed: " + ANSI_GREEN + passedTests + ANSI_RESET);
        System.out.println("Failed: " + ANSI_RED + (totalTests - passedTests) + ANSI_RESET);
        System.out.println("Success Rate: " +
                String.format("%.2f%%", (passedTests * 100.0 / totalTests)));
    }

    private static List<TestCase> createTestCases() {
        List<TestCase> tests = new ArrayList<>();

        // Test Case 1: Valid input with proper spacing
        TestCase test1 = new TestCase("ValidSpacingCase",
                "main\n" +
                        "num V_x , num V_y , num V_result ,\n" +
                        "begin\n" +
                        "  V_x < input ;\n" +
                        "  V_y < input ;\n" +
                        "  if eq ( V_y , 0 ) then\n" +
                        "    begin\n" +
                        "      V_result = F_average ( V_x , V_y , V_result ) ;\n" +
                        "      print V_result ;\n" +
                        "      skip ;\n" +
                        "    end\n" +
                        "  else\n" +
                        "    begin\n" +
                        "      if or ( eq ( V_x , 0 ) , eq ( V_y , 0 ) ) then\n" +
                        "        begin\n" +
                        "          print \"Zero\" ;\n" +
                        "        end\n" +
                        "      else\n" +
                        "        begin\n" +
                        "          V_result = mul ( V_x , V_y ) ;\n" +
                        "          return V_result ;\n" +
                        "        end ;\n" +
                        "    end ;\n" +
                        "end\n" +
                        "num F_average ( V_c , V_b , V_dummy )\n" +
                        "{\n" +
                        "num V_sum , num V_count , num V_cap ,\n" +
                        "begin\n" +
                        "  V_sum = 5 ;\n" +
                        "  V_count = 6 ;\n" +
                        "  V_sum = add ( V_sum , V_count ) ;\n" +
                        "  return V_count ;\n" +
                        "end\n" +
                        "}\n" +
                        "end");
        test1.expectedLexerSuccess = true;
        test1.expectedParserSuccess = true;
        tests.add(test1); // Add test1 to the list

        // Test Case 2: Input with lexer error (no space before semicolon)
        TestCase test2 = new TestCase("LexerErrorCase",
                "main\n" +
                        "num V_x , num V_y , num V_result\n" +
                        "begin\n" +
                        "  V_x < input;\n" + // This line causes lexer error
                        "  V_y < input ;\n");
        test2.expectedLexerSuccess = false;
        test2.expectedLexerErrors.add("Invalid token encountered: input;");
        tests.add(test2); // Add test2 to the list

        // Test Case 3: Parser error case
        TestCase test3 = new TestCase("ParserErrorCase",
                "main\n" +
                        "num V_x , num V_y , num V_result\n" +
                        "begin\n"); // Incomplete program
        test3.expectedParserSuccess = false;
        test3.expectedParserErrors.add("Error: No valid action found for token 'begin'");
        tests.add(test3); // Add test3 to the list

        // Test Case 4: Scope Error (Undeclared Function)
        TestCase scopeErrorTest = new TestCase("ScopeError",
            "main num V_x , num V_y , num V_result , begin    V_x < input ;   V_y < input ;    " +
            "if eq ( V_y , 0 ) then     begin       V_result = F_average ( V_x , V_y , V_result ) ;       " +
            "print V_result ;       skip ;     end   else     begin       if or ( eq ( V_x , 0 ) , eq ( V_y , 0 ) ) " +
            "then         begin           print \"Zero\" ;         end       else         begin           " +
            "V_result = mul ( V_x , V_y ) ;           return V_result ;         end ;     end ; end");
        scopeErrorTest.expectedScopeSuccess = false;
        scopeErrorTest.expectedScopeErrors.add("Error: Variable or function 'F_average' is not declared in the current scope.");

        // Test Case 5: Type Error
        TestCase typeErrorTest = new TestCase("TypeError",
            "main\n" +
            "num V_x , text V_y , num V_result ,\n" +
            "begin\n" +
            "  V_x < input ;\n" +
            "  V_y < input ;\n" +
            "  if eq ( V_y , 0 ) then\n" +
            "    begin\n" +
            "      V_result = F_average ( V_x , V_y , V_result ) ;\n" +
            "      print V_result ;\n" +
            "      skip ;\n" +
            "    end\n" +
            "  else\n" +
            "    begin\n" +
            "      if or ( eq ( V_x , 0 ) , eq ( V_y , 0 ) ) then\n" +
            "        begin\n" +
            "          print \"Zero\" ;\n" +
            "        end\n" +
            "      else\n" +
            "        begin\n" +
            "          V_result = mul ( V_x , V_y ) ;\n" +
            "          return V_result ;\n" +
            "        end ;\n" +
            "    end ;\n" +
            "end\n" +
            "num F_average ( V_c , V_b , V_dummy )\n" +
            "{\n" +
            "num V_sum , num V_count , num V_cap ,\n" +
            "begin\n" +
            "  V_sum = 5 ;\n" +
            "  V_count = 6 ;\n" +
            "  V_sum = add ( V_sum , V_count ) ;\n" +
            "  return V_count ;\n" +
            "end\n" +
            "}\n" +
            "end");
        typeErrorTest.expectedTypeSuccess = false;
        typeErrorTest.expectedTypeErrors.add("Line 8: Type Error: Arguments of eq must be of the same type");
        typeErrorTest.expectedTypeErrors.add("Line 10: Type Error: Parameter V_y must be of type num for function F_average");
        typeErrorTest.expectedTypeErrors.add("Line 16: Type Error: Arguments of eq must be of the same type");
        typeErrorTest.expectedTypeErrors.add("Line 22: Type Error: Arguments of mul must be numeric, found type: text");

        tests.add(scopeErrorTest);
        tests.add(typeErrorTest);

        // Full valid test case that should pass all phases including code generation
        TestCase fullTest = new TestCase("FullValidProgram",
                "main\n" +
                        "num V_x , num V_y , num V_result ,\n" +
                        "begin\n" +
                        "  V_x < input ;\n" +
                        "  V_y < input ;\n" +
                        "  if eq ( V_y , 0 ) then\n" +
                        "    begin\n" +
                        "      V_result = F_average ( V_x , V_y , V_result ) ;\n" +
                        "      print V_result ;\n" +
                        "      skip ;\n" +
                        "    end\n" +
                        "  else\n" +
                        "    begin\n" +
                        "      if or ( eq ( V_x , 0 ) , eq ( V_y , 0 ) ) then\n" +
                        "        begin\n" +
                        "          print \"Zero\" ;\n" +
                        "        end\n" +
                        "      else\n" +
                        "        begin\n" +
                        "          V_result = mul ( V_x , V_y ) ;\n" +
                        "          return V_result ;\n" +
                        "        end ;\n" +
                        "    end ;\n" +
                        "end\n" +
                        "num F_average ( V_c , V_b , V_dummy )\n" +
                        "{\n" +
                        "num V_sum , num V_count , num V_cap ,\n" +
                        "begin\n" +
                        "  V_sum = 5 ;\n" +
                        "  V_count = 6 ;\n" +
                        "  V_sum = add ( V_sum , V_count ) ;\n" +
                        "  return V_count ;\n" +
                        "end\n" +
                        "}\n" +
                        "end");

        tests.add(fullTest);
    

        return tests;
    }
    
    
    private static boolean runLexer(String inputFile) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "java",
                    "Main",
                    inputFile);

            // Redirect process output to capture it instead of printing
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();

            // Capture and store output but don't print it
            String output = captureOutput(process.getInputStream());

            int exitCode = process.waitFor();

            // Store output for error checking but don't print it
            boolean hasError = output.contains("Invalid token");

            return !hasError && exitCode == 0;
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }

    private static boolean runParser() {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "java",
                    "SLRParser");

            // Redirect process output to capture it instead of printing
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();

            // Capture and store output but don't print it
            String output = captureOutput(process.getInputStream());

            int exitCode = process.waitFor();

            // Store output for error checking but don't print it
            boolean hasError = output.contains("Parsing failed") ||
                    output.contains("Error: No valid action found");

            return !hasError && exitCode == 0;
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }

    private static String captureOutput(InputStream inputStream) throws IOException {
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        return output.toString();
    }

    private static boolean runTest(TestCase test) {
        try {
            System.out.println("\nTest Case: " + ANSI_BLUE + test.name + ANSI_RESET);

            // Write test input to file
            String inputFile = TEST_RESOURCES_DIR + test.name + "_input.txt";
            Files.write(Paths.get(inputFile), test.inputContent.getBytes());

            // Run lexer
            boolean lexerSuccess = runLexer(inputFile);
            boolean lexerResult = (lexerSuccess == test.expectedLexerSuccess);

            // Run parser if lexer succeeds
            boolean parserSuccess = false;
            boolean parserResult = true;
            if (lexerSuccess) {
                parserSuccess = runParser();
                parserResult = (parserSuccess == test.expectedParserSuccess);
            }

            // Run scope analyzer if parser succeeds
            boolean scopeSuccess = false;
            boolean scopeResult = true;
            if (parserSuccess) {
                scopeSuccess = runScopeAnalyzer();
                scopeResult = (scopeSuccess == test.expectedScopeSuccess);
            }

            // Run type checker if scope analysis succeeds
            boolean typeSuccess = false;
            boolean typeResult = true;
            if (scopeSuccess) {
                typeSuccess = runTypeChecker(inputFile);
                typeResult = (typeSuccess == test.expectedTypeSuccess);
            }

            // Run intermediate code generator if type checker succeeds
            boolean intermediateSuccess = false;
            if (typeSuccess) {
                intermediateSuccess = runIntermediateCodeGenerator(inputFile);
            }

            // Run target code generator if intermediate code generation succeeds
            boolean targetSuccess = false;
            if (intermediateSuccess) {
                targetSuccess = runTargetCodeGenerator("tc.txt");
            }

            printTestResults(test, lexerSuccess, lexerResult,
                    parserSuccess, parserResult,
                    scopeSuccess, scopeResult,
                    typeSuccess, typeResult,
                    intermediateSuccess, targetSuccess);

            return lexerResult && parserResult && scopeResult && typeResult;

        } catch (IOException e) {
            System.err.println("Test failed with IO error: " + e.getMessage());
            return false;
        }
    }

    private static boolean runIntermediateCodeGenerator(String inputFile) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "java",
                    "IntermediateCodeGenerator",
                    inputFile);
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();
            String output = captureOutput(process.getInputStream());
            int exitCode = process.waitFor();

            return exitCode == 0 && output.contains("Code generated successfully to intermediateCode.txt");
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }

    private static boolean runTargetCodeGenerator(String outputFile) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "java",
                    "TargetCode",
                    outputFile);
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();
            String output = captureOutput(process.getInputStream());
            int exitCode = process.waitFor();

            return exitCode == 0 &&
                    output.contains("Loading symbol table from Symbol.txt...") &&
                    output.contains("Reading intermediate code from intermediateCode.txt...") &&
                    output.contains("Generating BASIC code...") &&
                    output.contains("BASIC code has been written to");
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }

    private static void printTestResults(TestCase test,
            boolean lexerSuccess, boolean lexerResult,
            boolean parserSuccess, boolean parserResult,
            boolean scopeSuccess, boolean scopeResult,
            boolean typeSuccess, boolean typeResult,
            boolean intermediateSuccess,
            boolean targetSuccess) {
        System.out.println("\nResults:");

        // Previous checks...
        printComponentResult("Lexer", lexerSuccess, lexerResult, test.expectedLexerSuccess);
        if (lexerSuccess) {
            printComponentResult("Parser", parserSuccess, parserResult, test.expectedParserSuccess);
        }
        if (parserSuccess) {
            printComponentResult("Scope Analyzer", scopeSuccess, scopeResult, test.expectedScopeSuccess);
        }
        if (scopeSuccess) {
            printComponentResult("Type Checker", typeSuccess, typeResult, test.expectedTypeSuccess);
        }

        // Add code generation results
        if (typeSuccess) {
            printComponentResult("Intermediate Code Generator", intermediateSuccess, true, true);
        }
        if (intermediateSuccess) {
            printComponentResult("Target Code Generator", targetSuccess, true, true);
        }

        System.out.println("-".repeat(50));
    }

    private static boolean runScopeAnalyzer() {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "java",
                    "ScopeAnalyzer");
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();
            String output = captureOutput(process.getInputStream());
            int exitCode = process.waitFor();

            return exitCode == 0 && output.contains("Symbol table has been written to Symbol.txt");
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }

    private static boolean runTypeChecker(String inputFile) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "java",
                    "TypeChecker",
                    inputFile);
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();
            String output = captureOutput(process.getInputStream());
            int exitCode = process.waitFor();

            return exitCode == 0 && output.contains("No type errors found");
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }

    private static void printTestResults(TestCase test,
            boolean lexerSuccess, boolean lexerResult,
            boolean parserSuccess, boolean parserResult,
            boolean scopeSuccess, boolean scopeResult,
            boolean typeSuccess, boolean typeResult) {
        System.out.println("\nResults:");

        // Lexer Results
        printComponentResult("Lexer", lexerSuccess, lexerResult, test.expectedLexerSuccess);

        // Parser Results
        if (lexerSuccess) {
            printComponentResult("Parser", parserSuccess, parserResult, test.expectedParserSuccess);
        }

        // Scope Analyzer Results
        if (parserSuccess) {
            printComponentResult("Scope Analyzer", scopeSuccess, scopeResult, test.expectedScopeSuccess);
            if (!scopeSuccess && !test.expectedScopeErrors.isEmpty()) {
                System.out.println("Expected Scope Errors:");
                for (String error : test.expectedScopeErrors) {
                    System.out.println("  " + error);
                }
            }
        }

        // Type Checker Results
        if (scopeSuccess) {
            printComponentResult("Type Checker", typeSuccess, typeResult, test.expectedTypeSuccess);
            if (!typeSuccess && !test.expectedTypeErrors.isEmpty()) {
                System.out.println("Expected Type Errors:");
                for (String error : test.expectedTypeErrors) {
                    System.out.println("  " + error);
                }
            }
        }

        System.out.println("-".repeat(50));
    }

    private static void printComponentResult(String component, boolean success, boolean result, boolean expected) {
        String status = result ? ANSI_GREEN + "PASSED" : ANSI_RED + "FAILED";
        System.out.println(component + ": " + status + ANSI_RESET);
        System.out.println("  Expected Success: " + expected);
        System.out.println("  Actual Success: " + success);
    }
    private static void printTestResult(TestCase test, String inputFile,
            boolean lexerSuccess, boolean parserSuccess) {
        System.out.println("\nTest Case: " + ANSI_BLUE + test.name + ANSI_RESET);
        System.out.println("Input file: " + inputFile);

        // Lexer Results
        System.out.println("\nLexer Check:");
        if (test.expectedLexerSuccess) {
            if (lexerSuccess) {
                System.out.println(ANSI_GREEN + "✓ PASSED: Lexer succeeded as expected" + ANSI_RESET);
            } else {
                System.out.println(ANSI_RED + "✗ FAILED: Lexer failed but should have succeeded" + ANSI_RESET);
            }
        } else {
            if (lexerSuccess) {
                System.out.println(ANSI_RED + "✗ FAILED: Lexer succeeded but should have failed" + ANSI_RESET);
            } else {
                System.out.println(ANSI_GREEN + "✓ PASSED: Lexer failed as expected" + ANSI_RESET);
                // Check if it failed with the expected error
                for (String expectedError : test.expectedLexerErrors) {
                    System.out.println("  Expected error: " + expectedError);
                }
            }
        }

        // Parser Results (only if lexer should have succeeded)
        if (test.expectedLexerSuccess && lexerSuccess) {
            System.out.println("\nParser Check:");
            if (test.expectedParserSuccess) {
                if (parserSuccess) {
                    System.out.println(ANSI_GREEN + "✓ PASSED: Parser succeeded as expected" + ANSI_RESET);
                } else {
                    System.out.println(ANSI_RED + "✗ FAILED: Parser failed but should have succeeded" + ANSI_RESET);
                }
            } else {
                if (parserSuccess) {
                    System.out.println(ANSI_RED + "✗ FAILED: Parser succeeded but should have failed" + ANSI_RESET);
                } else {
                    System.out.println(ANSI_GREEN + "✓ PASSED: Parser failed as expected" + ANSI_RESET);
                    // Check if it failed with the expected error
                    for (String expectedError : test.expectedParserErrors) {
                        System.out.println("  Expected error: " + expectedError);
                    }
                }
            }
        }

        // Output files generated
        if (lexerSuccess) {
            System.out.println("\nOutput files generated:");
            System.out.println("- lexer.xml");
            if (parserSuccess) {
                System.out.println("- parser.xml");
            }
        }

        System.out.println("-".repeat(50));
    }

    
    private static void printResults(TestCase test, String inputFile,
            boolean lexerSuccess, boolean parserSuccess) {
        System.out.println("Input file: " + inputFile);
        System.out.println("\nResults:");

        // Lexer results
        String lexerResult = (lexerSuccess == test.expectedLexerSuccess) ? ANSI_GREEN + "PASSED" : ANSI_RED + "FAILED";
        System.out.println("Lexer: " + lexerResult + ANSI_RESET);

        // Parser results (if applicable)
        if (lexerSuccess) {
            String parserResult = (parserSuccess == test.expectedParserSuccess) ? ANSI_GREEN + "PASSED"
                    : ANSI_RED + "FAILED";
            System.out.println("Parser: " + parserResult + ANSI_RESET);
        }

        // Output files generated
        if (lexerSuccess) {
            System.out.println("\nOutput files generated:");
            System.out.println("- lexer.xml");
            if (parserSuccess) {
                System.out.println("- parser.xml");
            }
        }
    }

    private static void createDirectories() {
        try {
            Files.createDirectories(Paths.get(TEST_RESOURCES_DIR));
            Files.createDirectories(Paths.get(TEST_OUTPUT_DIR));
        } catch (IOException e) {
            System.err.println("Failed to create test directories: " + e.getMessage());
            System.exit(1);
        }
    }
}