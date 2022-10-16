package trie.nalim;

import one.nalim.Library;
import one.nalim.Link;
import one.nalim.Linker;

@Library("trie")
public class TrieAsm {
    

    @Link
    public static native int find_abs ( long array,  long key,  int len) ;


    static {
        Linker.linkClass(TrieAsm.class);
    }
}
