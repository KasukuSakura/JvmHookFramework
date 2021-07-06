package io.github.karlatemp.jhf.core.utils;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.PrintWriter;

public class DmpC {
    public static void dump(byte[] bytecode) {
        new ClassReader(bytecode)
                .accept(new TraceClassVisitor(
                        null,
                        new Textifier(),
                        new PrintWriter(System.out)
                ), 0);
    }
}
