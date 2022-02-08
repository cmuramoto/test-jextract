package test;

import java.io.IOException;

import jdk.incubator.foreign.MemorySegment;

public abstract class BaseReader {

	public static final long NO_VALUE = 1L << 32;
	public static final long ABSENT = 1L << 33;
	public static final long ABSENT_OR_NO_VALUE = NO_VALUE | ABSENT;

	public void run(JMHState state) {
		for (var i = 0; i < 10; i++) {
			seek(state.trie, state.keys, (int) state.keys.byteSize(), state.countTime);
		}
	}

	final void run() throws IOException {
		var state = new JMHState();
		state.setup();

		for (var i = 0; i < 10; i++) {
			seek(state.trie, state.keys, (int) state.keys.byteSize(), state.countTime);
		}
	}

	abstract void seek(MemorySegment trie, MemorySegment data, int len, long ct);

}
