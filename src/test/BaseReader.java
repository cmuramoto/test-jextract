package test;

import static jdk.incubator.foreign.ValueLayout.JAVA_BYTE;

import java.io.IOException;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.ResourceScope;

public abstract class BaseReader {

	public static final long NO_VALUE = 1L << 32;
	public static final long ABSENT = 1L << 33;
	public static final long ABSENT_OR_NO_VALUE = NO_VALUE | ABSENT;

	Path data = Paths.get("data/keys");

	Path trie = data.resolveSibling("trie");

	MemorySegment load(Path p, boolean copy) throws IOException {
		var scope = ResourceScope.newSharedScope();
		var ms = MemorySegment.mapFile(p, 0, Files.size(p), MapMode.READ_WRITE, scope);
		if (copy) {
			try (scope) {
				var locked = MemorySegment.allocateNative(ms.byteSize(), 4096, ResourceScope.globalScope());
				locked.copyFrom(ms);
				return locked;
			}
		} else {
			return ms;
		}
	}

	final void run() throws IOException {
		var data = load(this.data, true);
		var array = load(trie, true);

		var len = (int) data.byteSize();

		var c = count(data, len);

		for (var i = 0; i < 10; i++) {
			seek(array, data, len, c);
		}
	}

	final long count(MemorySegment data, int len) {
		var lines = 0;
		var now = System.nanoTime();
		for (var i = 0; i < len; i++) {
			if (data.get(JAVA_BYTE, i) == '\n') {
				lines++;
			}
		}
		var cl = System.nanoTime() - now;

		System.out.printf("(count) lines: %d. count time: %d.\n", lines, cl);
		return cl;
	}

	abstract void seek(MemorySegment trie, MemorySegment data, int len, long ct);

}
