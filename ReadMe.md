# Simple Programming Language Compiler

A robust compiler that translates custom programming language code into BASIC. This project implements a complete compilation pipeline with multiple phases of analysis and code generation.

## Table of Contents
- [Features](#features)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Usage](#usage)
- [Compilation Process](#compilation-process)
- [Input Format](#input-format)


## Features

- Complete compilation pipeline
- Interactive phase-by-phase execution
- Detailed error reporting
- BASIC code generation
- Support for custom programming syntax

## Prerequisites

- Java JDK 8 or higher
- Terminal/Command Prompt

## Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/compiler-project.git
   ```

2. Navigate to the project directory:
   ```bash
   cd compiler-project
   ```

3. Compile the source code:
   ```bash
   javac Compiler.java
   ```


### Basic Operation

1. Run the compiler:
   ```bash
   java Compiler
   ```

2. When prompted:
   - Enter the input file path (e.g., `input.txt`)
   - Enter the output file path (e.g., `output.bas`)

### Command-line Arguments (Optional)

```bash
java Compiler [input_file] [output_file]
```

Example:
```bash
java Compiler input.txt output.bas
```

## Compilation Process

The compiler executes the following phases in order:

1. **Lexical Analysis**
   - Tokenizes the input program
   - Identifies lexical errors

2. **Parsing**
   - Builds Abstract Syntax Tree (AST)
   - Validates syntax structure

3. **Scope Analysis**
   - Manages variable scope
   - Checks variable declarations

4. **Type Checking**
   - Validates type compatibility
   - Ensures type safety

5. **Intermediate Code Generation**
   - Creates intermediate representation
   - Optimizes code structure

6. **Target Code Generation**
   - Produces final BASIC code
   - Handles code optimization

## Input Format

### Syntax Requirements

- All tokens must be separated by spaces
- Each statement must end with a semicolon
- Variable names must start with 'V_'

### Correct Format Example

V_counter = 5 ;
V_result = V_counter + 10 ;

### Incorrect Format Example

V_counter=5;         # Missing spaces
V_result= V_counter+10;  # Inconsistent spacing

