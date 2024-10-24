# COS 341 Compiler 

## Table of Contents
- [Authors](#authors)
- [Prerequisites](#prerequisites)
- [Running the Compiler](#running-the-compiler)
- [Input File Format](#input-file-format)
- [Running Tests](#running-tests-optional)
- [Testing BASIC Output](#testing-basic-output)

## Authors
- Tessa Engelbrecht u22633601
- Teresa Park u22556908

## Prerequisites
- Java JDK 8 or higher
- Terminal/Command Prompt

## Running the Compiler

1. Run the JAR file: to run the compiler enter this command in the terminal
```bash
java -jar Compiler.jar
```

2. When prompted:
   - Enter the input program file path (e.g., `input.txt`)
   - Enter the output file path (e.g., `targetcode.bas`)
   - Follow the prompts to proceed through each compilation phase

## Input File Format

The input program must be in a `.txt` file with the following requirements:

- All tokens must be separated by spaces
- Each statement must end with a semicolon
- Variable names must start with 'V_'
- User defined function names must start with 'F_'

### Example Input Format (input.txt):
```txt
V_counter = 5 ;
V_result = V_counter + 10 ;
```

### Incorrect Format:
```txt
V_counter=5;         # Missing spaces
V_result= V_counter+10;  # Inconsistent spacing
```

## Running Tests (Optional)

To run the test suite run the following commands in the terminal:
```bash
javac CompilerTestRunner.java
java CompilerTestRunner
```
This will execute all test cases and display the results.

## Testing BASIC Output

The generated BASIC code has been tested using the AppleSoft BASIC emulator.
https://www.calormen.com/jsbasic/
- Paste the generated BASIC code in the window and then press Run