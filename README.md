# Crux Compiler

## Introduction
A project of the course, "Compilers & Interpreters". This is developed to compile and execute the programme written in Crux. For more information about this language, please visit [here](http://www.ics.uci.edu/~guoqingx/courses/142/ProjectGuide/crux.html).

## Getting started
> All the commands are tested on Mac OSX 10.11.1

### Compile
```bash
$ cd src

# for convenience, compile all available .java files
$ javac crux/*.java ast/*.java types/*.java
```

### Execute
```bash
$ cd src
$ java crux.Compiler file_to_compile.crx
```

## Things done
- Scanner
- Parser
- Symbol Table
- Abstract Parse Tree
- Type checking

## Things to be done
- Code generation & Optimization
