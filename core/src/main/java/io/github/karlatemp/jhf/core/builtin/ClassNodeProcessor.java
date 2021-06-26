package io.github.karlatemp.jhf.core.builtin;

import org.objectweb.asm.tree.ClassNode;

public interface ClassNodeProcessor {
    ClassNode transform(ClassNode node);
}
