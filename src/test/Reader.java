package test;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static test.JMHState.U;
import static test.UnalignedMemoryAccess.getIntAtOffset;

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

public class Reader extends BaseReader {

	static int base(MemorySegment ms, long offset) {
		return getIntAtOffset(ms, offset << 3);
	}

	static int check(MemorySegment ms, long offset) {
		return getIntAtOffset(ms, 4 + (offset << 3));
	}

	static int i32(long v) {
		return (int) v;
	}

	static long lookup(MemorySegment array, MemorySegment data, int pos, int end) {
		var from = 0L;
		var to = 0L;

		while (pos < end) {
			to = u64(base(array, from)) ^ u32(data.get(JAVA_BYTE, pos));
			if (check(array, to) != from) {
				return ABSENT;
			}
			from = to;
			pos++;
		}

		var b = base(array, from);
		var check = check(array, b);
		if (check != i32(from)) {
			return NO_VALUE;
		} else {
			return base(array, b);
		}
	}

	public static void main(String[] args) throws IOException {
		JMHState.VERBOSE = true;
		new Reader().run();
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
		var trie = state.trie;
		var keys = state.keys;
		var keyAddr = keys.address();
		var len = (int) keys.byteSize();
		for (var i = 0; i < len; i++) {
			if (U.getByte(keyAddr + i) == '\n') {
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
		var now = System.nanoTime();
		var keyAddr = keys.address();

		for (var i = 0; i < len; i++) {
			if (U.getByte(keyAddr + i) == '\n') {
				var q = lookup(trie, keys, start, i);

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
