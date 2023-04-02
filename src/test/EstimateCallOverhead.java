package test;

import static trie.asm.trie_h.noop;

import java.io.IOException;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.infra.Blackhole;

public class EstimateCallOverhead extends BaseReader {

	public static void main(String[] args) throws IOException {
		new EstimateCallOverhead().run();
	}

	@Benchmark
	@OutputTimeUnit(TimeUnit.NANOSECONDS)
	@OperationsPerInvocation(10_000_000)
	@BenchmarkMode(Mode.AverageTime)
	public void run(JMHState state, Blackhole bh) {
		var start = 0;
		var addr = state.trie.address();
		var keyAddr = state.keys.address();

		for (var i = 0; i < 10_000_000; i++) {
			bh.consume(noop(addr, keyAddr + start, i - start));
		}
	}

	@Override
	void seek(MemorySegment trie, MemorySegment ms, int len, long ct) {
		var start = 0;
		var lines = 0;
		var addr = trie.address();
		var data = ms.address();
		var now = System.nanoTime();

		for (var i = 0; i < len; i++) {
			if (ms.get(ValueLayout.JAVA_BYTE, i) == '\n') {
				var q = noop(addr, data + start, i - start);

				if (q != 0) {
					throw new Error();
				}

				start = i + 1;
				lines++;
			}
		}
		var dq = (double) (System.nanoTime() - now);

		System.out.printf("[%s](read) lines: %d. took: %.2f. minus count: %.2f. ns/call: %.2f. ns/call(-ct): %.2f\n", LocalDateTime.now(), lines, dq, dq - ct, dq / lines, (dq - ct) / lines);
	}
}
