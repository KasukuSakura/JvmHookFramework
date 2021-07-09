package io.github.karlatemp.jhf.core.builtin;

import io.github.karlatemp.jhf.api.markers.MarkerMirrorInitialize;
import io.github.karlatemp.jhf.api.utils.MapMirroredSet;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

public class ExtendsForbidden implements ClassNodeProcessor {
    public static final Collection<String> DENIED = new MapMirroredSet<>(new ConcurrentHashMap<>());
    public static final Collection<Predicate<String>> TESTERS = new MapMirroredSet<>(new ConcurrentHashMap<>());

    private static boolean isDenied(String name) {
        if (DENIED.contains(name)) return true;
        for (Predicate<String> t : TESTERS) {
            if (t.test(name)) return true;
        }
        return false;
    }

    @Override
    public ClassNode transform(ClassNode node) {
        if (isDenied(node.superName)) {
            node.superName = ("## Class Loading Rejected ## Class " + node.name + " cannot extends " + node.superName);
            node.name = node.superName;
            node.methods.clear();
            node.fields.clear();
            if (node.interfaces != null) {
                node.interfaces.clear();
            }
            node.access = Opcodes.ACC_PRIVATE;
        }
        return node;
    }

    static {
        for (Class<?> c : MarkerMirrorInitialize.ALLOCATED_CLASSES) {
            DENIED.add(c.getName().replace('.', '/'));
        }
        TESTERS.add(name -> name.startsWith("sun/reflect/") || name.startsWith("jdk/internal/reflect/"));
    }
}
