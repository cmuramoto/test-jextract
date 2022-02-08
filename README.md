# test-jextract

#### Example of linking assembly with java using jextract

The native folder contains a pre-built libtrie.so and source is already packaed with jextract generated files.

To regenerate just run ./build.asm.sh (requires nasm)

#### Benchmarks

~~The data folder contains a flat file (10 million keys) and a pre-populated trie that maps every key to its correspoding line number (string->int).~~

The data required to run the tests can be found in this [google-drive link](https://drive.google.com/drive/folders/1x-imW6MLZNdcJSolmG2B0B06OCYKLU8i). It consists of a flat file containing the keys to search and a pre-built [trie](https://github.com/cmuramoto/cedar-java), which just maps each keyword to its corresponding line number.

The benchmarks essentially loads both key and trie in memory and perform lookups for every key, which should all succeed.

To run java benchmarks just compile.sh and run

```
run.sh 
```

Standard Bencharks (MemorySegments, Unsafe and Native) should output something like

```
[2022-02-08T11:21:07.667884392](read) lines: 10000000. query time: 3546239079.00. dq-ct: 3427222419.00. ns/q: 354.62. ns/q(-ct): 342.72 ns/q(-ovh): 335.66 ns/q(-ovh -ct): 323.76
```

Where:

1. **query time**: the total time in nanoseconds to perform all 10mil lookups. This includes the estimate extra overhead of reading the keys file byte by byte and finding the line breaks in order to perform the queries
2. **dq - ct**: query time minus the estimate overhead for finding line breaks
3. **ns/q**: (query time)/lines - Average lookup cost
4. **ns/q(-ct)**: (dq -ct)/lines - Average lookup cost, w/out line break scan overhead
5. **ns/q(-ovh)**: (query time - lines*(native call overhead) (0 for non-native, 18ns for native))/lines - Average lookup cost, w/out native call overhead
6. **ns/q(-ovh - ct)**: (dq -ct - lines*(native call overhead))/lines - Average lookup cost, w/out neither native call overhead nor line break scan overhead. This should be close to the measure of pure lookup algorithimic performance

JMH benchmarks are performed with @BenchmarkMode(Mode.AverageTime) to mirror standard benchmarks and output should be similar to ns/q, since the samplings do not factor out runtime overhead.

To run cpp benchmarks

cd native
./compile.bench.sh shared (to link dinamically with libtrie.so)
./run.shared.sh

or

./compile.bench.sh (to link statically)
./bench
