package test;

import static jdk.incubator.foreign.ValueLayout.JAVA_BYTE;
import static trie.asm.trie_h.noop;

import java.io.IOException;
import java.time.LocalDateTime;

import jdk.incubator.foreign.MemorySegment;

public class EstimateCallOverhead extends BaseReader {

	@Override
	void seek(MemorySegment trie, MemorySegment ms, int len, long ct) {
		var start = 0;
		var lines = 0;
		var addr = trie.address().toRawLongValue();
		var data = ms.address().toRawLongValue();
		var now = System.nanoTime();

		for (var i = 0; i < len; i++) {
			if (ms.get(JAVA_BYTE, i) == '\n') {
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

	public static void main(String[] args) throws IOException {
		new EstimateCallOverhead().run();
	}
}
