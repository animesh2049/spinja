# SpinJa
SpinJa is a model checker for Promela, written in Java. Promela is the modelling language for the SPIN model checker. SpinJa supports a large subset of the Promela language.

SpinJa can be used to check for the absence of deadlocks, assertions, liveness properties and LTL properties (via never claims). SpinJa verification mode can exploit (nested) depth first search or breadth first search. Bitstate hashing and hash compaction modes are also supported. Furthermore, SPIN's partial order reduction and statement merging are implemented. SpinJa can also be used for simulation: random, interactive or guided.

SpinJa is designed to behave similarly to SPIN, but to be more easily extendible and reusable. From the start we have committed ourselves to Java and a clean object-oriented approach. Despite the fact that SpinJa uses a layered object-oriented design and is written in Java, SpinJa is not slow: benchmark experiments have shown that on average it is about 3 times slower than the highly optimized SPIN which uses C as implementation language.

SpinJa has been developed within the [Formal Methods and Tools](http://fmt.cs.utwente.nl/) (FMT) group of the [University of Twente](www.utwente.nl).

SpinJa is released under the [Apache 2.0 license](http://www.apache.org/licenses/LICENSE-2.0.html). 
# Files
The binary distribution of SpinJa (spinja-0.9.3-bin.zip) contains the following files:

    README.md: this file
    spinja.jar: SpinJa binary and library
    spinja.sh: SpinJa shell script
    doc/CHANGES.txt: revision history for SpinJa
    doc/LICENSE.txt: Apache 2.0 license
    tests/: directory with Promela examples

The source distribution of SpinJa (spinja-0.9.3-src.zip) contains two additional directories:

    lib/: additional Java library: the JavaCC compiler generator
    src/: complete Java source of SpinJa

SpinJa has been developed under Windows XP.
SpinJa 0.9.3 has been compiled under Ubuntu 12.04 using Java version 1.7.0_55.

# Compilation
If you have downloaded the binary distribution of SpinJa, you can skip this section: the binary distribution already contains a complete spinja.jar file.

To automatically build the spinja.jar, you need ant, the Java build tool.

    cd ./src/
    ant build

This will build spinja.jar into the ../build/ directory.

To generate the API documentation of SpinJa do the following:

    cd ./src/
    ant javadocs

This will build the Javadoc documentation of the SpinJa library into the ../doc/api/ directory.
# Installation
SpinJa requires Java version 1.7 or later.
The only file that is needed to run SpinJa is spinja.jar. This Java jar file contains the SpinJa Promela compiler and all utility code that is needed by the generated checker.

In the following we assume that spinja.jar resides in the current working directory. It is easier, however, to have spinja.jar available in Java's CLASSPATH environment variable.
# Running SpinJa

Like SPIN, verification with SpinJa is a three-step process: generation, compilation and execution:

    Generation. Use spinja.jar to generate a Java verification program for the Promela model sample.prom:

        java -cp spinja.jar spinja.Compile [options] sample.prom

    This generates the Java program ./spinja/PanModel.java.
    The optional options are SpinJa's compiler options (see below).

    Compilation. Use the javac compiler to compile the generated Java program to Java bytecode:

        javac -cp spinja.jar:. spinja/PanModel.java

    This will generate several Java class files in ./spinja/.

    Execution. Run the compiled Java program which checks the original Promela model:

        java -cp spinja.jar:. spinja.PanModel [options]

    The optional options are SpinJa's model checking options (see below).

After the verification, one can safely remove the generated ./spinja/ directory.
##### Shell script
To ease these tedious steps, a shell script is provided, which automates the whole process: spinja.sh. Verification of sample.prom is now straightforward:

    ./spinja.sh [options] sample.prom

The optional options will be passed to the SpinJa model checker.
SpinJa compiler options
For the generation of the Java program with spinja.Compile, SpinJa supports the following options to tune the generation process:

    -ntext: sets the name of the model to text (default: Pan).
    -o3: disables statement merging
    -stext: sets the output directory for the sourcecode to text (default: spinja)
    -v: shows diagnostic information on the compilation process.

#####  SpinJa model checker options
The generated SpinJa model checker supports several run-time options to guide the verification or simulation of the original Promela model.

**Verification**. In verification mode, SpinJa understands the following options to steer the verification process. Note that most options coincide with SPIN's C compiler options or pan's run-time options.

    -a: checks for acceptance cycles
    -A: assert statements will be ignored
    -b: exceeding the depth limit is considered an error
    -cn: stops the verification after N errors (default: 1); when set to 0, SpinJa will not stop for any error
    -DARRAY: uses a hash table that uses array lists instead of probing
    -DBFS: uses a Breadth First Search algorithm
    -DBITSTATE: uses bitstate hashing for storing states (approximate search)
    -DHC: uses hash compaction for storing states (approximate search)
    -DNOREDUCE: disables the partial order reduction algoritm
    -E: ignore invalid end states (deadlocks)
    -kn: sets N bits per state when using bitstate hashing (default: 3)
    -mn: sets the maximum search depth (default: 10000)
    -N: ignores any never claim (if present)
    -v: prints the version number and exits
    -wn: sets the number of entries in the hash table to 2^N (default: 21)

**Simulation**. Instead of a verification run, one can also use SpinJa's generated application for simulation. Like SPIN, SpinJa supports three simulation modes.

    -DGRANDOM: uses a randomizer to guide the search
    -DGTRAIL: uses the generated trail-file to guide the search
    -DGUSER: uses user input to guide the search

##### Java options
To configure the Java VM which runs the SpinJa model checker, one can supply additional options to the java program. The spinja.sh script, for example, currently uses the following Java options:

    -Xss16m: sets thread stack size to 16Mb,
    -Xms256m: sets the initial heap size to 256Mb,
    -Xmx1024m: sets the maximum heap size to 1024Mb.

Note that without such settings for the Java VM, Spinja would not be able to verify very large Promela models.
# Known issues
Promela
SpinJa does not yet support the full Promela language.
Currently, the following aspects of Promela are not yet implemented:

    unless statement
    communication: sorted send (!!), random receive (??), non-removing receive (<..>)
    labels within d_step and communication within d_step
    remote references: @labelname and :varname
    special variables: np_, pc_value, enabled
    Promela's C code extensions (e.g., c_code, c_expr)

##### Bugs

    When the hash table gets full (i.e., the parameter for -w was too low), the automatic resizing of the hash table is not always working properly. Work around: rerun the model checker with a higher value for -w.

# License
SpinJa is released under the Apache 2.0 license.
People

    Marc de Jonge: principal designer of SpinJa 0.8.
    Theo Ruys: supervisor of SpinJa project.


# Further Reading
For general documentation on Promela and the model checker SPIN, please consult the SPIN website, which hosts a wealth of information on the subjects. More specific information on the design and implementation of SpinJa can be found in the following two MSc theses.

    Marc de Jonge.
    The SpinJ Model Checker - A fast, extensible, object-oriented model checker.
    MSc Thesis, University of Twente, The Netherlands, September 2008.
    This MSc thesis describes the design and implementation of SpinJa 0.8 in detail.

    Mark Kattenbelt.
    Towards an Explicit-State Model Checking Framework.
    MSc Thesis, University of Twente, The Netherlands, September 2006.
    This MSc this presents a layered, object-oriented design for explicit-state model checkers. SpinJa's design is based on this approach.

# Version history
    0.9.3   2016.01.04 Spinja enhancement & bug fixes(Hitachi India Pvt Ltd)
    0.9     2010.04.10 	First public release of SpinJa (binary and source).
    0.8     2008.09.10 	Initial version of SpinJa after MSc Project of Marc de Jonge
# Release 0.9.3
    -Integrated relevant changes from SpinS developments that are applicable to SpinJa
    -Add new language features such as #include, #define, 
    -Handle the inline functions expressed in Promela
    -Fix the channel and process functionality in the sharing scenarios
    -Optimise the state snapshot retrieval by selective localisation
