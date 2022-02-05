#!/bin/bash


rm -rf trie ../src/trie/asm trie.o libtrie.so
nasm -felf64 trie.asm -o trie.o
ld -z noexecstack -shared trie.o -o libtrie.so
# t = package name, l=lib name to be used by System.loadLibrary(...) in generated code
/opt/java/jdk-19/bin/jextract -t trie.asm --source -ltrie trie.h

if [[ ! -d ../src/trie/asm ]]; then
  mkdir -p ../src/trie/asm
fi

mv trie/asm/* ../src/trie/asm

rm -rf trie