package io.github.karlatemp.jhf.api.utils;

public interface MethodInvokeStack {
    int getSize();

    Class<?> caller();
    void fastReturn();

    /// get, set
    Object getAsObject(int index);
    int getAsInt(int index);
    byte getAsByte(int index);
    boolean getAsBoolean(int index);
    float getAsFloat(int index);
    short getAsShort(int index);
    double getAsDouble(int index);
    long getAsLong(int index);
    char getAsChar(int index);

    void set(int index, Object value);
    void set(int index, int value);
    void set(int index, byte value);
    void set(int index, boolean value);
    void set(int index, float value);
    void set(int index, short value);
    void set(int index, double value);
    void set(int index, long value);
    void set(int index, char value);

}
