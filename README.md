# Crux Compiler

## Introduction
A project of the course, "Compilers & Interpreters". This is developed to compile and execute the programme written in Crux. For more information about this language, please visit [here](http://www.ics.uci.edu/~guoqingx/courses/142/ProjectGuide/crux.html).

## Getting started
> All the commands are tested on Mac OSX 10.11.3

### Compile the compiler
```bash
$ cd src

# for convenience, compile all available .java files
$ javac crux/*.java ast/*.java types/*.java mips/*.java
```

### Compile and Execute binary
```bash
$ cd src

# generate a file named file_to_compile.asm in the current folder
$ java crux.Compiler file_to_compile.crx

# execute the binary
$ spim -f file_to_compile.asm
```

## Things done
- Scanner
- Parser
- Symbol Table
- Abstract Parse Tree
- Type checking
- Code generation

## Backlog
- Optimization
