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

public class ReaderNativeAlt extends BaseReader {

	static final double CALL_OVERHEAD = 18.96d;

	@Override
	void seek(MemorySegment trie, MemorySegment keys, int len, long ct) {
		var addr = trie.address().toRawLongValue();
		var keyAddr = keys.address().toRawLongValue();
		var now = System.nanoTime();
		var lines = seek(addr, keyAddr, len);


		var dq = (double) (System.nanoTime() - now);

		var ovh = (lines * CALL_OVERHEAD);

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

                var q = lookup_key(trie, data, s - data, e - data);

                if ((q & ABSENT_OR_NO_VALUE) == 0) {
                    lines++;
                }
            }
            return lines;
        }
        
        static long findSep(long pos) {
            while (JMHState.U.getByte(pos) != '\n') {
                ++pos;
            }
            return pos;
        }
        
        static long lookup_key(long trie, long data, long start, long end) {
            return find_abs(trie, (data + start), (int)(end-start) );
        }

	public static void main(String[] args) throws IOException {
		JMHState.VERBOSE = true;
		new ReaderNativeAlt().run();
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
