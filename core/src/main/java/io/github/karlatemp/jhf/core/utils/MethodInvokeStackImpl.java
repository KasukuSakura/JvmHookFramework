package io.github.karlatemp.jhf.core.utils;

import io.github.karlatemp.jhf.api.utils.MethodInvokeStack;
import io.github.karlatemp.unsafeaccessor.Unsafe;
import org.objectweb.asm.Type;

public class MethodInvokeStackImpl implements MethodInvokeStack, MethodInvokeStackJLAMirror.MIS_JLA_MIRROR {
    protected long addressBase;
    protected long[] addresses;
    private final int size;
    protected final Class<?> caller;
    public boolean emittedReturnValue;
    protected Object[] objects;

    public static int getSize(Type arg) {
        switch (arg.getSort()) {
            case Type.INT:
                return Unsafe.ARRAY_INT_INDEX_SCALE;
            case Type.SHORT:
                return Unsafe.ARRAY_SHORT_INDEX_SCALE;
            case Type.BOOLEAN:
                return Unsafe.ARRAY_BOOLEAN_INDEX_SCALE;
            case Type.CHAR:
                return Unsafe.ARRAY_CHAR_INDEX_SCALE;
            case Type.BYTE:
                return Unsafe.ARRAY_BYTE_INDEX_SCALE;
            case Type.FLOAT:
                return Unsafe.ARRAY_FLOAT_INDEX_SCALE;
            case Type.DOUBLE:
                return Unsafe.ARRAY_DOUBLE_INDEX_SCALE;
            case Type.LONG:
                return Unsafe.ARRAY_LONG_INDEX_SCALE;
            default:
                throw new AssertionError(arg.toString());
        }
    }

    public static void caclMemSize(long[] sizePointer, long[][] addressPointer, Type returnType, Type[] arguments) {
        long size = 0;
        int objects = 0;
        long[] addresses = new long[arguments.length + (
                returnType.getSort() == Type.VOID ? 0 : 1
        )];

        for (int i = 0, argumentsLength = arguments.length; i < argumentsLength; i++) {
            Type arg = arguments[i];
            if (arg.getSort() == Type.OBJECT || arg.getSort() == Type.ARRAY) {
                addresses[i] = objects;
                objects++;
            } else {
                addresses[i] = size;
                size += getSize(arg);
            }
        }
        if (addresses.length != arguments.length) {
            if (returnType.getSort() == Type.OBJECT || returnType.getSort() == Type.ARRAY) {
                addresses[arguments.length] = objects;
                objects++;
            } else {
                addresses[arguments.length] = size;
                size += getSize(returnType);
            }
        }

        sizePointer[0] = size;
        sizePointer[1] = objects;
        addressPointer[0] = addresses;
    }

    public MethodInvokeStackImpl(
            long addressBase,
            long[] addresses,
            int size,
            int objCount,
            Class<?> caller
    ) {
        this.addressBase = addressBase;
        this.addresses = addresses;
        this.size = size;
        this.caller = caller;
        this.objects = new Object[objCount];
    }

    @Override
    public void fastReturn() {
        emittedReturnValue = true;
    }

    @Override
    public int getSize() {
        return size;
    }

    @Override
    public Class<?> caller() {
        return caller;
    }

    @Override
    public Object getAsObject(int index) {
        return objects[(int) addresses[index]];
//        return UAAccessHolder.UNSAFE.getReference(null, addressBase + addresses[index]);
    }

    @Override
    public int getAsInt(int index) {
        return UAAccessHolder.UNSAFE.getInt(addressBase + addresses[index]);
    }

    @Override
    public byte getAsByte(int index) {
        return UAAccessHolder.UNSAFE.getByte(addressBase + addresses[index]);
    }

    @Override
    public boolean getAsBoolean(int index) {
        return UAAccessHolder.UNSAFE.getBoolean(null, addressBase + addresses[index]);
    }

    @Override
    public float getAsFloat(int index) {
        return UAAccessHolder.UNSAFE.getFloat(addressBase + addresses[index]);
    }

    @Override
    public short getAsShort(int index) {
        return UAAccessHolder.UNSAFE.getShort(addressBase + addresses[index]);
    }

    @Override
    public double getAsDouble(int index) {
        return UAAccessHolder.UNSAFE.getDouble(addressBase + addresses[index]);
    }

    @Override
    public long getAsLong(int index) {
        return UAAccessHolder.UNSAFE.getLong(addressBase + addresses[index]);
    }

    @Override
    public char getAsChar(int index) {
        return UAAccessHolder.UNSAFE.getChar(addressBase + addresses[index]);
    }

    @Override
    public void set(int index, Object value) {
        objects[(int) addresses[index]] = value;
//        UAAccessHolder.UNSAFE.putReference(null, addressBase + addresses[index], value);
    }

    @Override
    public void set(int index, int value) {
        UAAccessHolder.UNSAFE.putInt(addressBase + addresses[index], value);
    }

    @Override
    public void set(int index, byte value) {
        UAAccessHolder.UNSAFE.putByte(addressBase + addresses[index], value);
    }

    @Override
    public void set(int index, boolean value) {
        UAAccessHolder.UNSAFE.putBoolean(null, addressBase + addresses[index], value);
    }

    @Override
    public void set(int index, float value) {
        UAAccessHolder.UNSAFE.putFloat(addressBase + addresses[index], value);
    }

    @Override
    public void set(int index, short value) {
        UAAccessHolder.UNSAFE.putShort(addressBase + addresses[index], value);
    }

    @Override
    public void set(int index, double value) {
        UAAccessHolder.UNSAFE.putDouble(addressBase + addresses[index], value);
    }

    @Override
    public void set(int index, long value) {
        UAAccessHolder.UNSAFE.putLong(addressBase + addresses[index], value);
    }

    @Override
    public void set(int index, char value) {
        UAAccessHolder.UNSAFE.putChar(addressBase + addresses[index], value);
    }

    // region JLA Access

    private int offset;

    @Override
    public void reset() {
        offset = 0;
    }

    @Override
    public void emit(int v) {
        set(offset++, v);
    }

    @Override
    public void emit(double v) {
        set(offset++, v);
    }

    @Override
    public void emit(short v) {
        set(offset++, v);
    }

    @Override
    public void emit(char v) {
        set(offset++, v);
    }

    @Override
    public void emit(float v) {
        set(offset++, v);
    }

    @Override
    public void emit(long v) {
        set(offset++, v);
    }

    @Override
    public void emit(boolean v) {
        set(offset++, v);
    }

    @Override
    public void emit(byte v) {
        set(offset++, v);
    }

    @Override
    public void emit(Object v) {
        set(offset++, v);
    }

    @Override
    public int poll_int() {
        return getAsInt(offset++);
    }

    @Override
    public double poll_double() {
        return getAsDouble(offset++);
    }

    @Override
    public short poll_short() {
        return getAsShort(offset++);
    }

    @Override
    public char poll_char() {
        return getAsChar(offset++);
    }

    @Override
    public float poll_float() {
        return getAsFloat(offset++);
    }

    @Override
    public long poll_long() {
        return getAsLong(offset++);
    }

    @Override
    public boolean poll_boolean() {
        return getAsBoolean(offset++);
    }

    @Override
    public byte poll_byte() {
        return getAsByte(offset++);
    }

    @Override
    public Object poll_Object() {
        return getAsObject(offset++);
    }

    @Override
    public void release() {
        UAAccessHolder.UNSAFE.freeMemory(addressBase);
        addresses = null;
        objects = null;
    }

    @Override
    public boolean isReturned() {
        return emittedReturnValue;
    }
    // endregion
}
