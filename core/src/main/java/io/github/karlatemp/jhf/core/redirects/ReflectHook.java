package io.github.karlatemp.jhf.core.redirects;

import io.github.karlatemp.jhf.api.utils.MethodInvokeStack;
import io.github.karlatemp.jhf.core.redirect.RedirectInfos;
import io.github.karlatemp.jhf.core.utils.DmpC;

import java.lang.reflect.*;

import static io.github.karlatemp.jhf.core.redirect.InvokeType.invokeStatic;
import static io.github.karlatemp.jhf.core.redirect.InvokeType.invokeVirtual;
import static io.github.karlatemp.jhf.core.utils.HighExceptionThrown.newJLRInaccessibleObjectException;

public class ReflectHook {
    @RedirectInfos(@RedirectInfos.Info(
            value = Class.class,
            methods = {
                    @RedirectInfos.MethodInfo(
                            invokeType = invokeStatic,
                            name = "forName",
                            methodDesc = "(Ljava/lang/String;)Ljava/lang/Class;"
                    ),
                    @RedirectInfos.MethodInfo(
                            invokeType = invokeStatic,
                            name = "forName",
                            methodDesc = "(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;"
                    )
            }
    ))
    public static void hookClassForName(MethodInvokeStack stack) throws ClassNotFoundException {
        // TODO
        System.out.println("[RH]: hook class invoked: " + stack.getAsObject(0));
    }


    @RedirectInfos(@RedirectInfos.Info(
            value = Field.class,
            methods = {
                    @RedirectInfos.MethodInfo(invokeType = invokeVirtual, name = "get", methodDesc = "(Ljava/lang/Object;)Ljava/lang/Object;"),
                    @RedirectInfos.MethodInfo(invokeType = invokeVirtual, name = "getBoolean", methodDesc = "(Ljava/lang/Object;)Z"),
                    @RedirectInfos.MethodInfo(invokeType = invokeVirtual, name = "getByte", methodDesc = "(Ljava/lang/Object;)B"),
                    @RedirectInfos.MethodInfo(invokeType = invokeVirtual, name = "getChar", methodDesc = "(Ljava/lang/Object;)C"),
                    @RedirectInfos.MethodInfo(invokeType = invokeVirtual, name = "getShort", methodDesc = "(Ljava/lang/Object;)S"),
                    @RedirectInfos.MethodInfo(invokeType = invokeVirtual, name = "getInt", methodDesc = "(Ljava/lang/Object;)I"),
                    @RedirectInfos.MethodInfo(invokeType = invokeVirtual, name = "getLong", methodDesc = "(Ljava/lang/Object;)J"),
                    @RedirectInfos.MethodInfo(invokeType = invokeVirtual, name = "getFloat", methodDesc = "(Ljava/lang/Object;)F"),
                    @RedirectInfos.MethodInfo(invokeType = invokeVirtual, name = "getDouble", methodDesc = "(Ljava/lang/Object;)D"),


                    @RedirectInfos.MethodInfo(invokeType = invokeVirtual, name = "set", methodDesc = "(Ljava/lang/Object;Ljava/lang/Object;)V"),
                    @RedirectInfos.MethodInfo(invokeType = invokeVirtual, name = "setBoolean", methodDesc = "(Ljava/lang/Object;Z)V"),
                    @RedirectInfos.MethodInfo(invokeType = invokeVirtual, name = "setByte", methodDesc = "(Ljava/lang/Object;B)V"),
                    @RedirectInfos.MethodInfo(invokeType = invokeVirtual, name = "setChar", methodDesc = "(Ljava/lang/Object;C)V"),
                    @RedirectInfos.MethodInfo(invokeType = invokeVirtual, name = "setShort", methodDesc = "(Ljava/lang/Object;S)V"),
                    @RedirectInfos.MethodInfo(invokeType = invokeVirtual, name = "setInt", methodDesc = "(Ljava/lang/Object;I)V"),
                    @RedirectInfos.MethodInfo(invokeType = invokeVirtual, name = "setLong", methodDesc = "(Ljava/lang/Object;J)V"),
                    @RedirectInfos.MethodInfo(invokeType = invokeVirtual, name = "setFloat", methodDesc = "(Ljava/lang/Object;F)V"),
                    @RedirectInfos.MethodInfo(invokeType = invokeVirtual, name = "setDouble", methodDesc = "(Ljava/lang/Object;D)V"),
            }
    ))
    public static void hookFieldGetSet(MethodInvokeStack stack) throws Throwable {
        Field field = (Field) stack.getAsObject(0);
        if (DmpC.GENERATED_CLASSES.contains(field.getDeclaringClass())) {
            throw new IllegalAccessException("PERMISSION DENIED");
        }
    }

    @RedirectInfos({
            @RedirectInfos.Info(
                    value = AccessibleObject.class,
                    methods = @RedirectInfos.MethodInfo(name = "setAccessible", methodDesc = "(Z)V", invokeType = invokeVirtual)
            ),
            @RedirectInfos.Info(
                    value = Method.class,
                    methods = @RedirectInfos.MethodInfo(name = "setAccessible", methodDesc = "(Z)V", invokeType = invokeVirtual)
            ),
            @RedirectInfos.Info(
                    value = Field.class,
                    methods = @RedirectInfos.MethodInfo(name = "setAccessible", methodDesc = "(Z)V", invokeType = invokeVirtual)
            ),
            @RedirectInfos.Info(
                    value = Constructor.class,
                    methods = @RedirectInfos.MethodInfo(name = "setAccessible", methodDesc = "(Z)V", invokeType = invokeVirtual)
            ),
    })
    public static void hookSetAccessible(MethodInvokeStack stack) throws Throwable {
        AccessibleObject ao = (AccessibleObject) stack.getAsObject(0);
        if (!(ao instanceof Member)) return;
        if (DmpC.GENERATED_CLASSES.contains(((Member) ao).getDeclaringClass())) {
            throw (Throwable) newJLRInaccessibleObjectException.invoke("PERMISSION DENIED, caller: " + stack.caller());
        }
    }

    @RedirectInfos(@RedirectInfos.Info(
            value = AccessibleObject.class,
            methods = @RedirectInfos.MethodInfo(name = "trySetAccessible", methodDesc = "()Z", invokeType = invokeVirtual)
    ))
    public static void hookTrySetAccessible(MethodInvokeStack stack) throws Throwable {
        AccessibleObject ao = (AccessibleObject) stack.getAsObject(0);
        if (!(ao instanceof Member)) return;
        if (DmpC.GENERATED_CLASSES.contains(((Member) ao).getDeclaringClass())) {
            stack.set(stack.getSize(), false);
            stack.fastReturn();
        }
    }
}
