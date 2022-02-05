package test;

import static jdk.incubator.foreign.ValueLayout.JAVA_BYTE;
import static trie.asm.trie_h.find_abs;

import java.io.IOException;
import java.time.LocalDateTime;

import jdk.incubator.foreign.MemorySegment;

public class ReaderNative extends BaseReader {

	static final double CALL_OVERHEAD = 8.91989021d;

	@Override
	void seek(MemorySegment trie, MemorySegment ms, int len, long ct) {
		var start = 0;
		var lines = 0;
		var addr = trie.address().toRawLongValue();
		var data = ms.address().toRawLongValue();
		var now = System.nanoTime();

		for (var i = 0; i < len; i++) {
			if (ms.get(JAVA_BYTE, i) == '\n') {
				var q = find_abs(addr, data + start, i - start);

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
		new ReaderNative().run();
	}
}
