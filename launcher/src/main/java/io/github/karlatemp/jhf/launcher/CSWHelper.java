package io.github.karlatemp.jhf.launcher;

import java.io.DataInput;
import java.io.DataOutput;
import java.util.ArrayList;
import java.util.List;

public class CSWHelper {
    public static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
    public static final int[] EMPTY_INT_ARRAY = new int[0];

    public interface Action<T, R> {
        public R run(T arg) throws Exception;
    }

    public interface ActionRt<R> {
        public R run() throws Exception;
    }

    public interface ActionVoid2<T, T2> {
        public void run(T arg, T2 arg2) throws Exception;
    }

    public static <T> void writeList(DataOutput output, List<T> list, ActionVoid2<T, DataOutput> action) throws Exception {
        output.writeShort(list.size());
        for (T t : list) {
            action.run(t, output);
        }
    }

    public static <T> List<T> readList(DataInput input, Action<DataInput, T> read) throws Exception {
        int size = input.readUnsignedShort();
        List<T> resp = new ArrayList<>(size);
        while (size-- > 0) {
            T v = read.run(input);
            resp.add(v);
        }
        return resp;
    }

    public static void writeArray(DataOutput output, byte[] bytes) throws Exception {
        output.writeInt(bytes.length);
        output.write(bytes);
    }

    public static byte[] readArray(DataInput input) throws Exception {
        int size = input.readInt();
        if (size == 0) return EMPTY_BYTE_ARRAY;
        byte[] rsp = new byte[size];
        input.readFully(rsp);
        return rsp;
    }

    public static void writeArray(DataOutput output, int[] bytes) throws Exception {
        output.writeInt(bytes.length);
        for (int i : bytes) {
            output.writeInt(i);
        }
    }

    public static int[] readIntArray(DataInput input) throws Exception {
        int size = input.readInt();
        if (size == 0) return EMPTY_INT_ARRAY;
        int[] rsp = new int[size];
        for (int i = 0; i < size; i++) {
            rsp[i] = input.readInt();
        }
        return rsp;
    }

}
