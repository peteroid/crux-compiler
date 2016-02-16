# Crux Compiler

## Introduction
A project of the course, "Compilers & Interpreters". This is developed to compile and execute the programme written in Crux. For more information about this language, please visit [here](http://www.ics.uci.edu/~guoqingx/courses/142/ProjectGuide/crux.html).

## Getting started
> All the commands are tested on Mac OSX 10.11.1

### Compile
```bash
$ cd src
$ javac crux/Compiler.java crux/Scanner.java crux/Token.java crux/NonTerminal.java crux/Parser.java crux/Symbol.java crux/SymbolTable.java
```

### Execute
```bash
$ java crux.Compiler file_to_compile.crx
```

## Things done
- Scanner
- Parser
- Symbol Table

## Things to be done
- Type checking
