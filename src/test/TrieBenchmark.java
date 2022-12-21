package test;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static test.UnalignedMemoryAccess.map;
import static trie.asm.trie_h.find_abs;

import java.io.IOException;
import java.lang.foreign.MemoryAddress;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import jdk.internal.misc.Unsafe;

@Fork(value = 3)
@BenchmarkMode(Mode.AverageTime)
@Warmup(iterations = 3, time = 500, timeUnit = TimeUnit.NANOSECONDS)
@Measurement(iterations = 3, time = 500, timeUnit = TimeUnit.NANOSECONDS)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@OperationsPerInvocation(10_000_000)
public class TrieBenchmark {

	static class AddressReader {
		int base(MemoryAddress ms, long offset) {
			return ms.get(JAVA_INT, offset << 3);
		}

		int check(MemoryAddress ms, long offset) {
			return ms.get(JAVA_INT, 4 + (offset << 3));
		}

		long lookup(MemoryAddress array, MemoryAddress data, int pos, int end) {
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

		public long seek(MemoryAddress trie, MemoryAddress data, int len) {
			var start = 0;
			var lines = 0;

			for (var i = 0; i < len; i++) {
				if (data.get(JAVA_BYTE, i) == '\n') {
					var q = lookup(trie, data, start, i);

					if ((q & ABSENT_OR_NO_VALUE) == 0) {
						lines++;
					}
					start = i + 1;
				}
			}
			return lines;
		}
	}

	static class BufferReader {

		int base(ByteBuffer ms, long offset) {
			return ms.getInt((int) (offset << 3));
		}

		int check(ByteBuffer ms, long offset) {
			return ms.getInt((int) (4 + (offset << 3)));
		}

		long lookup(ByteBuffer array, ByteBuffer data, int pos, int end) {
			var from = 0L;
			var to = 0L;

			while (pos < end) {
				to = u64(base(array, from)) ^ u32(data.get(pos));
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

		long seek(ByteBuffer trie, ByteBuffer data, int len) {
			var start = 0;
			var lines = 0;

			for (var i = 0; i < len; i++) {
				if (data.get(i) == '\n') {
					var q = lookup(trie, data, start, i);

					if ((q & ABSENT_OR_NO_VALUE) == 0) {
						lines++;
					}
					start = i + 1;
				}
			}
			return lines;
		}
	}

	static class NativeReader {

		int seek(MemoryAddress trie, MemoryAddress data, int len) {
			var start = 0;
			var lines = 0;
			var trieBase = trie.toRawLongValue();
			var dataBase = data.toRawLongValue();

			for (var i = 0; i < len; i++) {
				if (data.get(JAVA_BYTE, i) == '\n') {
					var q = find_abs(trieBase, dataBase + start, i - start);

					if ((q & ABSENT_OR_NO_VALUE) == 0) {
						lines++;
					}
					start = i + 1;
				}
			}
			return lines;
		}
	}

	static class NativeReaderAlt {

		static final Unsafe U;

		static {
			try {
				var f = Unsafe.class.getDeclaredField("theUnsafe");
				f.setAccessible(true);
				U = (Unsafe) f.get(null);
			} catch (Throwable e) {
				throw new ExceptionInInitializerError(e);
			}
		}

		static long findSep(long pos) {
			while (U.getByte(pos) != '\n') {
				++pos;
			}
			return pos;
		}

		static long lookup_key(long trie, long data, long start, long end) {
			return find_abs(trie, (data + start), (int) (end - start));
		}

		int seek(long trie, long data, int len) {
			var lines = 0;
			var start = data;
			var end = data;
			var tail = data + len;
			for (; end != tail; start = ++end) {
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
	}

	static class ReaderUnsafe {
		static final Unsafe U;

		static {
			try {
				var f = Unsafe.class.getDeclaredField("theUnsafe");
				f.setAccessible(true);
				U = (Unsafe) f.get(null);
			} catch (Throwable e) {
				throw new ExceptionInInitializerError(e);
			}
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

		long seek(long trie, long data, int len) {
			var start = 0;
			var lines = 0;

			for (var i = 0; i < len; i++) {
				if (U.getByte(data + i) == '\n') {
					var q = lookup(trie, data, start, i);

					if ((q & ABSENT_OR_NO_VALUE) == 0) {
						lines++;
					}
					start = i + 1;
				}
			}
			return lines;
		}
	}

	static class ReaderUnsafeAlt {
		static final Unsafe U;

		static {
			try {
				var f = Unsafe.class.getDeclaredField("theUnsafe");
				f.setAccessible(true);
				U = (Unsafe) f.get(null);
			} catch (Throwable e) {
				throw new ExceptionInInitializerError(e);
			}
		}

		static long findSep(long pos) {
			while (U.getByte(pos) != '\n') {
				++pos;
			}
			return pos;
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

		int seek(long trie, long data, int len) {
			var lines = 0;
			var start = data;
			var end = data;
			var tail = data + len;
			for (; end != tail; start = ++end) {
				end = findSep(end);

				var q = lookup(trie, data, (int) (start - data), (int) (end - data));

				if ((q & ABSENT_OR_NO_VALUE) == 0) {
					lines++;
				}
			}
			return lines;
		}
	}

	static class SegmentReader {
		int base(MemorySegment ms, long offset) {
			return ms.get(JAVA_INT, offset << 3);
		}

		int check(MemorySegment ms, long offset) {
			return ms.get(JAVA_INT, 4 + (offset << 3));
		}

		long lookup(MemorySegment array, MemorySegment data, int pos, int end) {
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

		public long seek(MemorySegment trie, MemorySegment data, int len) {
			var start = 0;
			var lines = 0;

			for (var i = 0; i < len; i++) {
				if (data.get(JAVA_BYTE, i) == '\n') {
					var q = lookup(trie, data, start, i);

					if ((q & ABSENT_OR_NO_VALUE) == 0) {
						lines++;
					}
					start = i + 1;
				}
			}
			return lines;
		}
	}

	static Path dataPath = Paths.get("data/keys");

	static Path triePath = dataPath.resolveSibling("trie");
	static MemorySegment data = load(dataPath, true);
	static MemorySegment trie = load(triePath, true);

	static final long NO_VALUE = 1L << 32;

	static final long ABSENT = 1L << 33;

	static final long ABSENT_OR_NO_VALUE = NO_VALUE | ABSENT;

	static int i32(long v) {
		return (int) v;
	}

	static MemorySegment load(Path p, boolean copy) {
		try {
			var scope = MemorySession.openShared();
			var ms = map(p, 0, Files.size(p), scope);
			if (copy) {
				try (scope) {
					var locked = MemorySegment.allocateNative(ms.byteSize(), 4096, MemorySession.global());
					locked.copyFrom(ms);
					return locked;
				}
			} else {
				return ms;
			}
		} catch (IOException e) {
			throw new ExceptionInInitializerError();
		}
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
	public long address_seek() {
		return new AddressReader().seek(trie.address(), data.address(), (int) data.byteSize());
	}

	@Benchmark
	public long buffer_seek() {
		return new BufferReader().seek(trie.asByteBuffer().order(ByteOrder.LITTLE_ENDIAN), data.asByteBuffer().order(ByteOrder.LITTLE_ENDIAN), (int) data.byteSize());
	}

	@Benchmark
	public long native_seek() {
		return new NativeReader().seek(trie.address(), data.address(), (int) data.byteSize());
	}

	@Benchmark
	public long native_seek_alt() {
		return new NativeReaderAlt().seek(trie.address().toRawLongValue(), data.address().toRawLongValue(), (int) data.byteSize());
	}

	@Benchmark
	public long segment_seek() {
		return new SegmentReader().seek(trie, data, (int) data.byteSize());
	}

	@Benchmark
	public long unsafe_seek() {
		return new ReaderUnsafe().seek(trie.address().toRawLongValue(), data.address().toRawLongValue(), (int) data.byteSize());
	}

	@Benchmark
	public long unsafe_seek_alt() {
		return new ReaderUnsafeAlt().seek(trie.address().toRawLongValue(), data.address().toRawLongValue(), (int) data.byteSize());
	}
}
