package test;

import static jdk.incubator.foreign.ValueLayout.JAVA_BYTE;
import static jdk.incubator.foreign.ValueLayout.JAVA_INT;

import java.io.IOException;
import java.time.LocalDateTime;

import jdk.incubator.foreign.MemorySegment;

import jdk.internal.misc.Unsafe;

public class ReaderUnsafe extends BaseReader {

	static final Unsafe U;

	static {
		try {
			var f = Unsafe.class.getDeclaredField("theUnsafe");
			f.setAccessible(true);
			U = (Unsafe)f.get(null);
		} catch (Throwable e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	@Override
	void seek(MemorySegment trie, MemorySegment data, int len, long ct) {
		var start = 0;
		var lines = 0;
		var addr = trie.address().toRawLongValue();
		var base = data.address().toRawLongValue();
		var now = System.nanoTime();

		for (var i = 0; i < len; i++) {
			if (data.get(JAVA_BYTE, i) == '\n') {
				var q = lookup(addr, base, start, i);

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

	public static void main(String[] args) throws IOException {
		new ReaderUnsafe().run();
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

	int base(MemorySegment ms, long offset) {
		return ms.get(JAVA_INT, offset << 3);
	}

	int check(MemorySegment ms, long offset) {
		return ms.get(JAVA_INT, 4 + (offset << 3));
	}

	long lookup(long trie, long data, int pos, int end) {
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
}
