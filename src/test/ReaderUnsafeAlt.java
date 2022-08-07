package test;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.infra.Blackhole;

import jdk.incubator.foreign.MemorySegment;

public class ReaderUnsafeAlt extends BaseReader {

	@Override
	void seek(MemorySegment trie, MemorySegment keys, int len, long ct) {
		var addr = trie.address().toRawLongValue();
		var keyAddr = keys.address().toRawLongValue();
		var now = System.nanoTime();

		var lines = seek(addr, keyAddr, len);
		var dq = (double) (System.nanoTime() - now);

		var ovh = 0L;

		System.out.printf("[%s](read) lines: %d. query time: %.2f. dq-ct: %.2f. ns/q: %.2f. ns/q(-ct): %.2f ns/q(-ovh): %.2f ns/q(-ovh -ct): %.2f\n", LocalDateTime.now(), lines, dq, dq - ct, dq / lines, (dq - ct) / lines, (dq - ovh) / lines, (dq - ovh - ct) / lines);
	}


        int seek(long trie, long data, int len) {
            var lines = 0;
            var start = data;
            var end = data;
            var tail = data + len;
            for ( ; end != tail ; start = ++end) {
                end = findSep(end);

                var s = start;
                var e = end;

                var q = lookup(trie, data, s - data, e - data);

                if ((q & ABSENT_OR_NO_VALUE) == 0) {
                    lines++;
                }
            }
            return lines;
        }


	public static void main(String[] args) throws IOException {
		JMHState.VERBOSE = true;
		new ReaderUnsafeAlt().run();
	}

	static long u64(int n) {
		// this is the right conversion, however the code itself it not ready to deal with negative
		// values, so keep the upcast as a signed promotion.
		// return n & 0XFFFFFFFFL;
		return n;
	}

	static int u32(byte v) {
		return v & 0xFF;
	}

	static int i32(long v) {
		return (int) v;
	}

	static long lookup(long trie, long data, long pos, long end) {
		var from = 0L;
		var to = 0L;
		var addr_4 = trie + 4;

		for (; pos < end; pos++) {
			to = JMHState.U.getInt(trie + (from << 3)) ^ u32(JMHState.U.getByte(data + pos));
			if (JMHState.U.getInt(addr_4 + (to << 3)) != from) {
				return ABSENT;
			}

			from = to;
		}

		to = JMHState.U.getLong(trie + (JMHState.U.getInt(trie + (from << 3)) << 3));

		if ((to >>> 32) != from) {
			return NO_VALUE;
		}

		return to & 0xFFFFFFFFL;
	}

	static long findSep(long pos) {
            while (JMHState.U.getByte(pos) != '\n') {
                ++pos;
            }
            return pos;
        }

	@Benchmark
	@OutputTimeUnit(TimeUnit.NANOSECONDS)
	@OperationsPerInvocation(10_000_000)
	@BenchmarkMode(Mode.AverageTime)
	public void run(JMHState state, Blackhole bh) {
		var trie = state.trie.address().toRawLongValue();
		var keys = state.keys.address().toRawLongValue();
		var len = (int) state.keys.byteSize();
		var lines = seek(trie, keys, len);
	}
}
