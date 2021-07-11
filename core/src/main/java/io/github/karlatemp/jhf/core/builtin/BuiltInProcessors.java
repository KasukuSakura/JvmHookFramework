package io.github.karlatemp.jhf.core.builtin;

import io.github.karlatemp.jhf.api.event.EventPriority;
import io.github.karlatemp.jhf.api.events.TransformBytecodeEvent;
import io.github.karlatemp.jhf.core.mixin.JHFBytecodeProvider;
import io.github.karlatemp.jhf.core.redirect.RedirectGenerator;
import io.github.karlatemp.jhf.core.redirects.ReflectHook;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.util.ArrayList;
import java.util.List;

public class BuiltInProcessors {
    private static void preinit() throws Exception {
        PCChain.processors.add(new ExtendsForbidden());

        PCChain.processors.add(new RedirectedClassNodeProcessor(RedirectGenerator.redirectInfos));

        RedirectGenerator.generate(ReflectHook.class);
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
            if (JHFBytecodeProvider.LOADING_N == null) {
                JHFBytecodeProvider.LOADING_N = node.name;
            }
            node = chain.transform(node);
            ClassWriter writer = new ClassWriter(0);
            node.accept(writer);
            event.bytecode = writer.toByteArray();
        });
    }
}
