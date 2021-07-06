package io.github.karlatemp.jhf.core.redirects;

import io.github.karlatemp.jhf.api.utils.MethodInvokeStack;
import io.github.karlatemp.jhf.core.redirect.InvokeType;
import io.github.karlatemp.jhf.core.redirect.RedirectInfos;

public class ReflectHook {
    @RedirectInfos(@RedirectInfos.Info(
            value = Class.class,
            methods = {
                    @RedirectInfos.MethodInfo(
                            invokeType = InvokeType.invokeStatic,
                            name = "forName",
                            methodDesc = "(Ljava/lang/String;)Ljava/lang/Class;"
                    ),
                    @RedirectInfos.MethodInfo(
                            invokeType = InvokeType.invokeStatic,
                            name = "forName",
                            methodDesc = "(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;"
                    )
            }
    ))
    public static void hookClassForName(MethodInvokeStack stack) throws ClassNotFoundException {
        // TODO
    }
}
