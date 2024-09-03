// package parser;

// public class Main {
//     public static void main(String[] args) {
//         if (args.length < 2) {
//             System.out.println("Usage: java Main <input-xml-file> <output-xml-file>");
//             return;
//         }

//         Parser parser = new Parser();
//         try {
//             parser.parseInputXML(args[0]);
//             parser.parse();
//             parser.writeOutputXML(args[1]);
//             System.out.println("Parsing complete. Output written to " + args[1]);
//         } catch (Exception e) {
//             System.err.println("Error during parsing: " + e.getMessage());
//             e.printStackTrace();
//         }
//     }
// }