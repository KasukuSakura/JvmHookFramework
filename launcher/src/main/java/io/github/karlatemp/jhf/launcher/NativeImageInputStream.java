package io.github.karlatemp.jhf.launcher;

import sun.misc.Unsafe;

import java.io.IOException;
import java.io.InputStream;

class NativeImageInputStream extends InputStream {
    private long pointer;
    private long size;

    NativeImageInputStream(long pointer, long size) {
        this.pointer = pointer;
        this.size = size;
    }

    @Override
    public int available() throws IOException {
        if (pointer == 0) return -1;
        return (int) size;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (size == 0 || pointer == 0) return -1;
        if (len > size) len = (int) size;
        if (len < 0 || off < 0 || off + len < 0 || off + len > b.length)
            throw new IOException("Illegal argument");

        if (Unsafe.ARRAY_BYTE_INDEX_SCALE == 1) {
            JHFLauncher.unsafe.copyMemory(null, pointer, b, Unsafe.ARRAY_BYTE_BASE_OFFSET + off, len);
        } else {
            long basePointer = pointer;
            for (int i = 0; i < len; i++) {
                b[i] = JHFLauncher.unsafe.getByte(basePointer + i);
            }
        }

        size -= len;
        pointer += len;
        return len;
    }

    @Override
    public int read() throws IOException {
        if (pointer == 0) return -1;
        if (size == 0) return -1;
        size--;
        return Byte.toUnsignedInt(JHFLauncher.unsafe.getByte(pointer++));
    }

    @Override
    public void close() throws IOException {
        size = 0;
        pointer = 0;
    }
}
