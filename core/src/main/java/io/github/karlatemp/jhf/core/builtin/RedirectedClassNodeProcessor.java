package io.github.karlatemp.jhf.core.builtin;

import io.github.karlatemp.jhf.core.utils.RedirectInfos;
import org.objectweb.asm.tree.ClassNode;

import java.util.Collection;

public class RedirectedClassNodeProcessor implements ClassNodeProcessor {
    private final Collection<RedirectInfos.RedirectInfo> inf;

    public RedirectedClassNodeProcessor(Collection<RedirectInfos.RedirectInfo> inf) {
        this.inf = inf;
    }

    @Override
    public ClassNode transform(ClassNode node) {
        RedirectInfos.applyRedirect(node, inf);
        return node;
    }
}
