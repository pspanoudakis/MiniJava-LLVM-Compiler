## MiniJava to LLVM-IR Compiler

This was a Spring 2021 semester project for the University of Athens
[Compilers course](http://cgi.di.uoa.gr/~compilers/index.html).

Written in Java, it accepts a [MiniJava](http://cgi.di.uoa.gr/~compilers/project_files/minijava-new/minijava.html)
file, performs Semantic checking and generates an equivalent
[LLVM-IR](https://llvm.org/docs/LangRef.html) file that can be compiled to an executable binary
by [Clang](https://clang.llvm.org/).

### Dependencies
- `make`
- Java (most likely any version >= 8 will do the job)
- [JavaCC](https://javacc.github.io/javacc/) 5, along with a University of Athens
[patched version](http://cgi.di.uoa.gr/~compilers/tools/jtb132di.jar)
of [JTB](http://compilers.cs.ucla.edu/jtb/) 1.3.2 are used for Parsing and Parse Tree generation.
(The `.jar` files for both are included, but can be found [here](http://cgi.di.uoa.gr/~compilers/tools.html) as well.)

### Compilation/Execution instructions

To build:
- In the project root, `cd MiniJavaLLVMCompiler`
- Run `make`.

To compile one or multiple files, run `java Main <file> <rest files>*`.

To clean up all generated files when done, run `make clean`.

### AST Visitors
This program takes advantage of Visitor Pattern. 4 Visitors are used in the below order:
- **ClassNameCollector**:
Collects and stores all class names declared in the file.
- **Declaration Collector**:
Collects and stores all fields and methods declared in classes.
- **FunctionBodyAnalyzer**:
Performs static checking in method bodies, while taking into account all the stored
information up to that point. If an error is detected, it is reported and
the compilation of the current target file will is aborted.
- **IRGenerator**:
Creates the output LLVM-IR file.

### Development & Testing
Developed and tested in WSL Ubuntu 20.04, using Visual Studio Code.
- `javacc5.jar` and `jtb132di.jar` files were used for JavaCC and JTB respectively.
- Java SE-14 was used in development & testing.
