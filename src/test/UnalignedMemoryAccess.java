package test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.foreign.ValueLayout.OfBoolean;
import java.lang.foreign.ValueLayout.OfByte;
import java.lang.foreign.ValueLayout.OfChar;
import java.lang.foreign.ValueLayout.OfDouble;
import java.lang.foreign.ValueLayout.OfFloat;
import java.lang.foreign.ValueLayout.OfInt;
import java.lang.foreign.ValueLayout.OfLong;
import java.lang.foreign.ValueLayout.OfShort;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public interface UnalignedMemoryAccess {

	static OfBoolean BOOL_U = ValueLayout.JAVA_BOOLEAN;
	static OfByte BYTE_U = ValueLayout.JAVA_BYTE;
	static OfShort SHORT_U = ValueLayout.JAVA_SHORT.withBitAlignment(8);
	static OfChar CHAR_U = ValueLayout.JAVA_CHAR.withBitAlignment(8);
	static OfInt INT_U = ValueLayout.JAVA_INT.withBitAlignment(8);
	static OfFloat FLOAT_U = ValueLayout.JAVA_FLOAT.withBitAlignment(8);
	static OfLong LONG_U = ValueLayout.JAVA_LONG.withBitAlignment(8);
	static OfDouble DOUBLE_U = ValueLayout.JAVA_DOUBLE.withBitAlignment(8);

	static byte getByteAtOffset(MemorySegment ms, long off) {
		return ms.get(BYTE_U, off);
	}

	static int getIntAtOffset(MemorySegment ms, long off) {
		return ms.get(INT_U, off);
	}

	public static long getLongAtIndex(MemorySegment ms, long ix) {
		return getLongAtOffset(ms, ix << 3);
	}

	static long getLongAtOffset(MemorySegment ms, long off) {
		return ms.get(LONG_U, off);
	}

	static short getShortAtOffset(MemorySegment ms, long off) {
		return ms.get(SHORT_U, off);
	}

	static MemorySegment map(Path path, long base, long size) {
		return map(path, base, size, Arena.openShared());
	}

	static MemorySegment map(Path path, long base, long size, Arena ms) {
		return map(path, base, size, ms, true);
	}

	@SuppressWarnings("preview")
	static MemorySegment map(Path path, long base, long size, Arena ms, boolean closeOnFail) {
		try (var fc = FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.READ)) {
			return fc.map(MapMode.READ_WRITE, base, size, ms.scope());
		} catch (IOException e) {
			if (closeOnFail) {
				ms.close();
			}
			throw new UncheckedIOException(e);
		}
	}

	static MemorySegment map(Path path, long base, Arena ms) {
		return map(path, base, ms, true);
	}

	static MemorySegment map(Path path, long base, Arena ms, boolean closeOnFail) {
		try {
			return map(path, base, Files.size(path), ms, closeOnFail);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	static void setByteAtOffset(MemorySegment ms, long off, byte v) {
		ms.set(BYTE_U, off, v);
	}

	static void setIntAtOffset(MemorySegment ms, long off, int v) {
		ms.set(INT_U, off, v);
	}

	public static void setLongAtIndex(MemorySegment ms, long ix, long v) {
		setLongAtOffset(ms, ix << 3, v);
	}

	static void setLongAtOffset(MemorySegment ms, long off, long v) {
		ms.set(LONG_U, off, v);
	}

	static void setShortAtOffset(MemorySegment ms, long off, short v) {
		ms.set(SHORT_U, off, v);
	}
}
