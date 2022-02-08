package test;

import java.io.IOException;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.IntStream;

import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.ResourceScope;

@State(Scope.Benchmark)
public class JMHState {

	static boolean VERBOSE = false;

	static final jdk.internal.misc.Unsafe U;

	static {
		try {
			var f = jdk.internal.misc.Unsafe.class.getDeclaredField("theUnsafe");
			f.setAccessible(true);
			U = (jdk.internal.misc.Unsafe) f.get(null);
		} catch (Throwable e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	Path keysPath = Paths.get("data/keys");

	Path triePath = keysPath.resolveSibling("trie");

	MemorySegment keys;

	MemorySegment trie;

	long countTime;

	@Setup(Level.Trial)
	public void setup() throws IOException {
		if (keys == null || trie == null) {
			keys = load(keysPath, true);
			trie = load(triePath, true);

			countTime = IntStream.range(0, 10).mapToLong(v -> count(keys)).min().orElseThrow();

			if (VERBOSE) {
				System.out.printf("(count) lines: %d. count time: %d.\n", 10_000_000, countTime);
			}
		}
	}

	@TearDown(Level.Trial)
	public void release() {
		if (keys != null) {
			keys.scope().close();
		}

		if (trie != null) {
			trie.scope().close();
		}

		if (VERBOSE) {
			System.out.println("Closed scopes");
		}
	}

	MemorySegment load(Path p, boolean copy) throws IOException {
		var scope = ResourceScope.newSharedScope();
		var ms = MemorySegment.mapFile(p, 0, Files.size(p), MapMode.READ_WRITE, scope);
		if (copy) {
			try (scope) {
				var locked = MemorySegment.allocateNative(ms.byteSize(), 4096, ResourceScope.newSharedScope());
				locked.copyFrom(ms);
				return locked;
			}
		} else {
			return ms;
		}
	}

	static final long count(MemorySegment keys) {
		var lines = 0;
		var now = System.nanoTime();
		var addr = keys.address().toRawLongValue();
		int len = (int) keys.byteSize();
		for (var i = 0; i < len; i++) {
			if (U.getByte(addr + i) == '\n') {
				lines++;
			}
		}
		var cl = System.nanoTime() - now;

		return cl;
	}
}
