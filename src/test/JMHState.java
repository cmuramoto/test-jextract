package test;

import static test.UnalignedMemoryAccess.map;

import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.IntStream;

import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

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

	static final long count(MemorySegment keys) {
		var lines = 0;
		var now = System.nanoTime();
		var addr = keys.address();
		int len = (int) keys.byteSize();
		for (var i = 0; i < len; i++) {
			if (U.getByte(addr + i) == '\n') {
				lines++;
			}
		}
		var cl = System.nanoTime() - now;

		return cl;
	}

	Path keysPath = Paths.get("data/keys");

	Path triePath = keysPath.resolveSibling("trie");

	MemorySegment keys;

	MemorySegment trie;

	long countTime;

	MemorySegment load(Path p, boolean copy) throws IOException {
		var scope = Arena.openShared();
		var ms = map(p, 0, Files.size(p), scope);
		if (copy) {
			try (scope) {
				var locked = MemorySegment.allocateNative(ms.byteSize(), 4096, Arena.openShared().scope());
				locked.copyFrom(ms);
				return locked;
			}
		} else {
			return ms;
		}
	}

	@TearDown(Level.Trial)
	public void release() {
		if (keys != null) {
			// keys.scope().close();
			keys = null; // TBD jdk 20 Arena
		}

		if (trie != null) {
			// trie.session().close();
			trie = null;
		}

		if (VERBOSE) {
			System.out.println("Closed scopes");
		}
	}

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
}
