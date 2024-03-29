package test;

import static test.JMHState.U;

import java.io.IOException;
import java.lang.foreign.MemorySegment;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.infra.Blackhole;

public class ReaderUnsafe extends BaseReader {

	static int i32(long v) {
		return (int) v;
	}

	static long lookup(long trie, long data, int pos, int end) {
		var from = 0L;
		var to = 0L;
		var addr_4 = trie + 4;

		for (; pos < end; pos++) {
			to = U.getInt(trie + (from << 3)) ^ u32(U.getByte(data + pos));
			if (U.getInt(addr_4 + (to << 3)) != from) {
				return ABSENT;
			}

			from = to;
		}

		to = U.getLong(trie + (U.getInt(trie + (from << 3)) << 3));

		if ((to >>> 32) != from) {
			return NO_VALUE;
		}

		return to & 0xFFFFFFFFL;
	}

	public static void main(String[] args) throws IOException {
		JMHState.VERBOSE = true;
		new ReaderUnsafe().run();
	}

	static int u32(byte v) {
		return v & 0xFF;
	}

	static long u64(int n) {
		// this is the right conversion, however the code itself it not ready to deal with negative
		// values, so keep the upcast as a signed promotion.
		// return n & 0XFFFFFFFFL;
		return n;
	}

	@Benchmark
	@OutputTimeUnit(TimeUnit.NANOSECONDS)
	@OperationsPerInvocation(10_000_000)
	@BenchmarkMode(Mode.AverageTime)
	public void run(JMHState state, Blackhole bh) {
		var start = 0;
		var lines = 0;
		var trie = state.trie.address();
		var keys = state.keys.address();
		var len = (int) state.keys.byteSize();
		for (var i = 0; i < len; i++) {
			if (U.getByte(keys + i) == '\n') {
				var q = lookup(trie, keys, start, i);
				bh.consume(q);
				if ((q & ABSENT_OR_NO_VALUE) == 0) {
					lines++;
				}
				start = i + 1;
			}
		}
	}

	@Override
	void seek(MemorySegment trie, MemorySegment keys, int len, long ct) {
		var start = 0;
		var lines = 0;
		var addr = trie.address();
		var keyAddr = keys.address();
		var now = System.nanoTime();

		for (var i = 0; i < len; i++) {
			if (U.getByte(keyAddr + i) == '\n') {
				var q = lookup(addr, keyAddr, start, i);

				if ((q & ABSENT_OR_NO_VALUE) == 0) {
					lines++;
				}
				start = i + 1;
			}
		}
		var dq = (double) (System.nanoTime() - now);

		var ovh = 0L;

		System.out.printf("[%s](read) lines: %d. query time: %.2f. dq-ct: %.2f. ns/q: %.2f. ns/q(-ct): %.2f ns/q(-ovh): %.2f ns/q(-ovh -ct): %.2f\n", LocalDateTime.now(), lines, dq, dq - ct, dq / lines, (dq - ct) / lines, (dq - ovh) / lines, (dq - ovh - ct) / lines);
	}

}
