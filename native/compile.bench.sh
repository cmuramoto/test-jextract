#!/bin/bash

if [[ $1 == 'shared' ]]; then
  echo "linking dynamically"
  g++ -L$PWD -O2 -Wall -o bench.shared bench.cpp -ltrie
else
  echo "linking statically"
  g++ -O2 -o bench bench.cpp trie.o
fi


