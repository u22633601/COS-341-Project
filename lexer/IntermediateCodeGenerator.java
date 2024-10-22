import java.io.*;
import java.util.*;

public class IntermediateCodeGenerator {

    // HashMap to store the symbol table with new variable/function names
    private static Map<String, String> symbolTable = new HashMap<>();

    public static void main(String[] args) {
        try {
            // Step 1: Load the symbol table
            loadSymbolTable("Symbol.txt");

            // Step 2: Read the input code
            List<String> inputLines = readInputCode("input.txt");

            // Step 3: Generate the intermediate code
            StringBuilder intermediateCode = new StringBuilder();

            // Step 4: Process the input code line by line
            for (String line : inputLines) {
                String translatedLine = translateLine(line);
                intermediateCode.append(translatedLine).append("\n");
            }

            intermediateCode.append("STOP\n");

            // Step 5: Output the intermediate code
            System.out.println("Intermediate Code:");
            System.out.println(intermediateCode.toString());

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    // Function to load the symbol table from the file
    private static void loadSymbolTable(String symbolFile) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(symbolFile));
        String line;
        while ((line = reader.readLine()) != null) {
            String[] parts = line.split(" : ");
            if (parts.length == 3) {
                String vname = parts[0].trim();
                String newName = parts[1].trim();
                symbolTable.put(vname, newName);
            }
        }
        reader.close();
    }

    // Function to read the input code from a file
    private static List<String> readInputCode(String inputFile) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(inputFile));
        List<String> lines = new ArrayList<>();
        String line;
        while ((line = reader.readLine()) != null) {
            lines.add(line.trim());
        }
        reader.close();
        return lines;
    }

    // Function to translate a single line of input code into intermediate code
    private static String translateLine(String line) {
        if (line.contains("return")) {
            // Translation for return
            String variable = line.substring(line.indexOf("return") + 6).trim();
            return "RETURN " + translateAtomic(variable);
        } else if (line.contains("halt")) {
            return "STOP";
        } else if (line.contains("< input")) {
            // Translation for assignment with input
            String variable = line.substring(0, line.indexOf("< input")).trim();
            return "INPUT " + translateAtomic(variable);
        } else if (line.contains("=")) {
            // Translation for assignment (including function calls)
            String[] parts = line.split("=");
            String variable = parts[0].trim();
            String term = parts[1].trim();
            if (term.contains("(") && term.contains(")")) {
                // It's a function call, translate accordingly
                return translateAtomic(variable) + " := " + translateCall(term);
            } else {
                // Simple assignment
                return translateAtomic(variable) + " := " + translateTerm(term);
            }
        } else if (line.contains("skip")) {
            return "REM DO NOTHING";
        }
        return "";
    }

    // Translation for atomic variables or constants
    private static String translateAtomic(String atomic) {
        // Check if it's a string constant
        if (atomic.startsWith("\"") && atomic.endsWith("\"")) {
            return atomic; // Return string constants as-is
        }

        // Look up the new name in the symbol table, return original if not found
        String translated = symbolTable.getOrDefault(atomic, atomic);

        // Debugging log
        System.out.println("Translating atomic: " + atomic + " -> " + translated);

        return translated;
    }

    // Translate terms (could be a simple atomic or an operation)
    private static String translateTerm(String term) {
        return translateAtomic(term); // In this simple case, we assume terms are atomic
    }

    // Translate function calls with varying parameters
    private static String translateCall(String call) {
        // Extract function name and parameters
        String functionName = call.substring(0, call.indexOf("(")).trim();
        String params = call.substring(call.indexOf("(") + 1, call.indexOf(")")).trim();

        // Split parameters and translate each
        String[] paramList = params.split(",");
        StringBuilder callTranslation = new StringBuilder("CALL_" + translateAtomic(functionName) + "(");

        for (int i = 0; i < paramList.length; i++) {
            callTranslation.append(translateAtomic(paramList[i].trim()));
            if (i < paramList.length - 1) {
                callTranslation.append(", ");
            }
        }

        callTranslation.append(")");

        // Return the correct function call translation
        return callTranslation.toString();
    }
}
