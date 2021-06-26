package io.github.karlatemp.jhf.core.builtin;

import io.github.karlatemp.jhf.api.event.EventPriority;
import io.github.karlatemp.jhf.api.events.TransformBytecodeEvent;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.util.ArrayList;
import java.util.List;

public class BuiltInProcessors {
    private static void preinit() throws Exception {
        PCChain.processors.add(new ReflectionLimit());
        PCChain.processors.add(new JLReflectInvoke());
    }

    static class PCChain implements ClassNodeProcessor {
        static final List<ClassNodeProcessor> processors = new ArrayList<>();

        @Override
        public ClassNode transform(ClassNode node) {
            for (ClassNodeProcessor processor : processors) {
                ClassNode nx = processor.transform(node);
                if (nx != null) node = nx;
            }
            return node;
        }
    }


    public static void setup() throws Exception {
        preinit();
        PCChain chain = new PCChain();
        TransformBytecodeEvent.EVENT_LINE.register(EventPriority.HIGH, event -> {
            ClassNode node = new ClassNode();
            new ClassReader(event.bytecode).accept(node, 0);
            node = chain.transform(node);
            ClassWriter writer = new ClassWriter(0);
            node.accept(writer);
            event.bytecode = writer.toByteArray();
        });
    }
}
