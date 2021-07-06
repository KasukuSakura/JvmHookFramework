package io.github.karlatemp.jhf.core.utils;

import io.github.karlatemp.unsafeaccessor.Unsafe;
import io.github.karlatemp.unsafeaccessor.UnsafeAccess;

public class UAAccessHolder {
    public static final UnsafeAccess UA = UnsafeAccess.getInstance();
    public static final Unsafe UNSAFE = Unsafe.getUnsafe();
}
