## MiniJava to LLVM-IR Compiler

This was a Spring 2021 semester project for the University of Athens
[Compilers course](http://cgi.di.uoa.gr/~compilers/index.html).

Written in Java, it accepts a [MiniJava](http://cgi.di.uoa.gr/~compilers/project_files/minijava-new/minijava.html)
file, performs Semantic checking and generates an equivalent
[LLVM-IR](https://llvm.org/docs/LangRef.html) file that can be compiled to an executable binary
by [Clang](https://clang.llvm.org/).

### Dependencies
- `make`
- Java (any version >= 8 will do the job)
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

### Parse Tree Visitors
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

### Class Information
- In a `ClassInfo` object, the class name, superclass, fields,
methods and offsets are stored. The visitors handle a `Map` structure
containing one such object for each class in the file.
- Class fields are also stored in a `Map` structure containing
`FieldInfo` objects. Each `FieldInfo` has its respective offset and a `VariableInfo`
object, which is essentially a pair of `name` and `type` strings.
- Class methods are stored in a `Map` structure as well, which consists of
`MethodInfo` objects. Each `MethodInfo` has its respective offset and a `FunctionInfo`
object, which contains the method `name`, return `type` and parameter types.

### Virtual Table
A `VirtualTable` object maps Method names to Method objects. All classes in the MiniJava file an object of this type.
When a class extends a superclass, it provides the Virtual Table to the superclass (recursively),
to store its methods first, and the subclass methods are added afterwards. If a method overrides
a superclass method, it replaces it and obtains its offset.

### Symbol Table
- The Symbol Table only stores local variables and class fields, since methods can be
looked-up in `ClassInfo` objects, and uses a Stack (`Deque`) of ScopeSymbols.
When entering a Class, all fields (including superclass fields) are pushed in the Stack:
If e.g. `class B extends A`, class `A` fields will be pushed first, and class `B` fields will be pushed
afterwards. When analyzing a class `B` method, all parameters and local variables will be pushed,
and when done, they will be popped. When we are done with the class, the fields will also be popped.
- When an identifier name is looked-up in the Symbol Table, we start searching the first `ScopeSymbols` object
in the Stack and then search the rest until an entry associated with that name is found.

### FunctionBodyAnalyzer
- Each `visit` method in this visitor returns a `String`, which indicates the type of the evaluated expression,
or `null`, if the statement does not need to be evaluated (loops, declarations etc.).
The Visitor stores:
- A `Map` (`classInfos`), used by the previous Visitors
to store Classes along with their fields and methods,
- A `currentClass` slot were the Class of the Method that is currently being visited is stored.
- A Symbol Table, used as described above.

### IRGenerator
- Each `visit` method in this visitor returns a `String`, which is the name of the register where
the evaluated expression is stored, or `null`, if this is not needed.  
The second argument is also a `String`, which essentially is a slot for extra information to be
provided to `visit(Identifier, String)` method:
    - If the argument is `null`, then the method returns the name of the identifier.
    - If the argument is `"lvalue"`, then the method returns the stack register that refers to the
    variable (and essentially contains the variable address).
    - If the argument is `"rvalue"`, then the variable of the above register is loaded into a new 
    register, which contains the variable value, and this new register is returned.

The Visitor stores:
- The `classInfos` `Map` used by the previous visitors,
- A `currentClass` slot were the Class of the Method that is currently being visited is stored.
This allows to quickly determine the class type of the object stored in `%this`.
- A Symbol Table, used as described above,
- A `FileWriter` used to write to output `.ll` file,
- Counters used for generating new register and label names,
- A `Map` (`objectRegisters`) which connects registers (which point to **objects**) to the Class of their object. This is useful in method calls (`MessageSend`'s), to determine the class type of the caller object (and therefore decide which class `VirtualTable` to lookup to get the method offset). This maps local function registers, therefore it is cleared as soon as the function body has been generated.

### Development & Testing
Developed and tested in WSL Ubuntu 20.04, using Visual Studio Code.
- `javacc5.jar` and `jtb132di.jar` files were used for JavaCC and JTB respectively.
- Java SE-14 was used in development & testing.
