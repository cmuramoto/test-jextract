# test-jextract

#### Example of linking assembly with java using jextract

The native folder contains a pre-built libtrie.so and source is already packaed with jextract generated files.

To regenerate just run ./build.asm.sh (requires nasm)

#### Benchmarks

The data folder contains a flat file (10 million keys) and a pre-populated trie that maps every key to its correspoding line number (string->int).

The benchmarks essentially loads both key and trie in memory and perform lookups for every key, which should all succeed.

To run java benchmarks just compile and run one of

1. run.memseg.sh (lookup using memory segments)
2. run.unsafe.sh (lookup using unsafe)
3. run.native.sh (lookup using native calls to the assembly library)

To run cpp benchmarks

cd native
./compile.bench.sh shared (to link dinamically with libtrie.so)
./run.shared.sh

or

./compile.bench.sh (to link statically)
./bench
