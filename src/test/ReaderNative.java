package test;

import static trie.asm.trie_h.find_abs;

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

public class ReaderNative extends BaseReader {

	static final double CALL_OVERHEAD = 18.96d;

	@Override
	void seek(MemorySegment trie, MemorySegment keys, int len, long ct) {
		var start = 0;
		var lines = 0;
		var addr = trie.address().toRawLongValue();
		var keyAddr = keys.address().toRawLongValue();
		var now = System.nanoTime();

		for (var i = 0; i < len; i++) {
			if (JMHState.U.getByte(keyAddr + i) == '\n') {
				var q = find_abs(addr, keyAddr + start, i - start);

				if ((q & ABSENT_OR_NO_VALUE) == 0) {
					lines++;
				}
				start = i + 1;
			}
		}
		var dq = (double) (System.nanoTime() - now);

		var ovh = (lines * CALL_OVERHEAD);

		System.out.printf("[%s](read) lines: %d. query time: %.2f. dq-ct: %.2f. ns/q: %.2f. ns/q(-ct): %.2f ns/q(-ovh): %.2f ns/q(-ovh -ct): %.2f\n", LocalDateTime.now(), lines, dq, dq - ct, dq / lines, (dq - ct) / lines, (dq - ovh) / lines, (dq - ovh - ct) / lines);
	}

	public static void main(String[] args) throws IOException {
		JMHState.VERBOSE = true;
		new ReaderNative().run();
	}

	@Benchmark
	@OutputTimeUnit(TimeUnit.NANOSECONDS)
	@OperationsPerInvocation(10_000_000)
	@BenchmarkMode(Mode.AverageTime)
	public void run(JMHState state, Blackhole bh) {
		var start = 0;
		var lines = 0;
		var trie = state.trie.address().toRawLongValue();
		var keys = state.keys.address().toRawLongValue();
		var len = (int) state.keys.byteSize();
		for (var i = 0; i < len; i++) {
			if (JMHState.U.getByte(keys + i) == '\n') {
				var q = find_abs(trie, keys + start, i - start);
				bh.consume(q);
				if ((q & ABSENT_OR_NO_VALUE) == 0) {
					lines++;
				}
				start = i + 1;
			}
		}
	}
}
