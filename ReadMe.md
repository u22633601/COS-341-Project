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

# PLEASE NOTE
- Our Lexer only accepts input strings that have spaces at the end of each token.
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

### Example input
- We have included an example input for your reference called input.txt. 

## Prerequisites
- Java SE Development Kit (JDK) 22.0.2 or compatible version
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


## Running Tests (Optional)

To run the test suite run the following commands in the terminal:
```bash
java -jar Testing.jar
```
This will execute all test cases and display the results.

The test_resources has the testing files used in the 6 different tests.

# Testing BASIC Output

The generated BASIC code has been tested using the AppleSoft BASIC emulator.
https://www.calormen.com/jsbasic/
- Paste the generated BASIC code in the window and then press Run
