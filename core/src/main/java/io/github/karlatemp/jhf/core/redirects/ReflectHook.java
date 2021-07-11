package io.github.karlatemp.jhf.core.redirects;

import io.github.karlatemp.jhf.api.utils.MethodInvokeStack;
import io.github.karlatemp.jhf.core.redirect.RedirectInfos;
import io.github.karlatemp.jhf.core.utils.DmpC;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.*;

import static io.github.karlatemp.jhf.core.redirect.InvokeType.invokeStatic;
import static io.github.karlatemp.jhf.core.redirect.InvokeType.invokeVirtual;
import static io.github.karlatemp.jhf.core.utils.HighExceptionThrown.newJLIllegalAccessException;
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

    private static void checkClassAccess(MethodInvokeStack stack, Object source, Class<?> c, MethodHandle exceptionAlloc) throws Throwable {
        if (DmpC.GENERATED_CLASSES.contains(c)) {
            throw (Throwable) exceptionAlloc.invoke("PERMISSION DENIED, not allowed access '" + source + "' from " + stack.caller());
        }
    }

    private static void checkMemberAccess(MethodInvokeStack stack, Member member, MethodHandle exceptionAlloc) throws Throwable {
        checkClassAccess(stack, member, member.getDeclaringClass(), exceptionAlloc);
        if (member instanceof Field && member.getDeclaringClass() == MethodHandles.Lookup.class) {
            throw (Throwable) exceptionAlloc.invoke("Now allowed set accessible of " + member);
        }
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
        checkMemberAccess(stack, field, newJLIllegalAccessException);
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
        checkMemberAccess(stack, (Member) ao, newJLRInaccessibleObjectException);
    }

    @RedirectInfos(@RedirectInfos.Info(
            value = AccessibleObject.class,
            methods = @RedirectInfos.MethodInfo(invokeType = invokeStatic, name = "setAccessible", methodDesc = "([Ljava/lang/reflect/AccessibleObject;Z)V")
    ))
    public static void hookSetAccessible1(MethodInvokeStack stack) throws Throwable {
        for (AccessibleObject object : (AccessibleObject[]) stack.getAsObject(0)) {
            if (object instanceof Member) {
                checkMemberAccess(stack, (Member) object, newJLRInaccessibleObjectException);
            }
        }
    }

    @RedirectInfos(@RedirectInfos.Info(
            value = AccessibleObject.class,
            methods = @RedirectInfos.MethodInfo(name = "trySetAccessible", methodDesc = "()Z", invokeType = invokeVirtual)
    ))
    public static void hookTrySetAccessible(MethodInvokeStack stack) throws Throwable {
        AccessibleObject ao = (AccessibleObject) stack.getAsObject(0);
        if (!(ao instanceof Member)) return;
        Member member = (Member) ao;
        if (member instanceof Field && member.getDeclaringClass() == MethodHandles.Lookup.class) {
            stack.set(stack.getSize(), false);
            stack.fastReturn();
            return;
        }
        if (DmpC.GENERATED_CLASSES.contains(member.getDeclaringClass())) {
            stack.set(stack.getSize(), false);
            stack.fastReturn();
        }
    }

    @RedirectInfos(@RedirectInfos.Info(
            value = MethodHandles.Lookup.class,
            methods = {
                    @RedirectInfos.MethodInfo(invokeType = invokeVirtual, name = "unreflect", methodDesc = "(Ljava/lang/reflect/Method;)Ljava/lang/invoke/MethodHandle;"),
                    @RedirectInfos.MethodInfo(invokeType = invokeVirtual, name = "unreflectSpecial", methodDesc = "(Ljava/lang/reflect/Method;Ljava/lang/Class;)Ljava/lang/invoke/MethodHandle;"),
                    @RedirectInfos.MethodInfo(invokeType = invokeVirtual, name = "unreflectConstructor", methodDesc = "(Ljava/lang/reflect/Constructor;)Ljava/lang/invoke/MethodHandle;"),
                    @RedirectInfos.MethodInfo(invokeType = invokeVirtual, name = "unreflectGetter", methodDesc = "(Ljava/lang/reflect/Field;)Ljava/lang/invoke/MethodHandle;"),
                    @RedirectInfos.MethodInfo(invokeType = invokeVirtual, name = "unreflectSetter", methodDesc = "(Ljava/lang/reflect/Field;)Ljava/lang/invoke/MethodHandle;"),
                    @RedirectInfos.MethodInfo(invokeType = invokeVirtual, name = "unreflectVarHandle", methodDesc = "(Ljava/lang/reflect/Field;)Ljava/lang/invoke/VarHandle;"),
            }
    ))
    public static void hookUnreflect(MethodInvokeStack stack) throws Throwable {
        checkMemberAccess(stack, (Member) stack.getAsObject(1), newJLIllegalAccessException);
    }

    @RedirectInfos(@RedirectInfos.Info(
            value = MethodHandles.Lookup.class,
            methods = {
                    @RedirectInfos.MethodInfo(invokeType = invokeVirtual, name = "findStatic", methodReturnType = MethodHandle.class, methodParameters = {Class.class, String.class, MethodType.class}),
                    @RedirectInfos.MethodInfo(invokeType = invokeVirtual, name = "findVirtual", methodReturnType = MethodHandle.class, methodParameters = {Class.class, String.class, MethodType.class}),
                    @RedirectInfos.MethodInfo(invokeType = invokeVirtual, name = "findConstructor", methodReturnType = MethodHandle.class, methodParameters = {Class.class, MethodType.class}),
                    @RedirectInfos.MethodInfo(invokeType = invokeVirtual, name = "accessClass", methodReturnType = Class.class, methodParameters = {Class.class}),
                    @RedirectInfos.MethodInfo(invokeType = invokeVirtual, name = "findSpecial", methodReturnType = MethodHandle.class, methodParameters = {Class.class, String.class, MethodType.class, Class.class}),
                    @RedirectInfos.MethodInfo(invokeType = invokeVirtual, name = "findGetter", methodReturnType = MethodHandle.class, methodParameters = {Class.class, String.class, Class.class}),
                    @RedirectInfos.MethodInfo(invokeType = invokeVirtual, name = "findSetter", methodReturnType = MethodHandle.class, methodParameters = {Class.class, String.class, Class.class}),
                    @RedirectInfos.MethodInfo(invokeType = invokeVirtual, name = "findVarHandle", methodDesc = "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/invoke/VarHandle;"),
                    @RedirectInfos.MethodInfo(invokeType = invokeVirtual, name = "findStaticGetter", methodReturnType = MethodHandle.class, methodParameters = {Class.class, String.class, Class.class}),
                    @RedirectInfos.MethodInfo(invokeType = invokeVirtual, name = "findStaticSetter", methodReturnType = MethodHandle.class, methodParameters = {Class.class, String.class, Class.class}),
                    @RedirectInfos.MethodInfo(invokeType = invokeVirtual, name = "findStaticVarHandle", methodDesc = "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/invoke/VarHandle;"),
            }
    ))
    public static void hookLookupFindMethods(MethodInvokeStack stack) throws Throwable {
        checkClassAccess(stack, stack.getAsObject(1), (Class<?>) stack.getAsObject(1), newJLIllegalAccessException);
    }


    @RedirectInfos(@RedirectInfos.Info(
            value = MethodHandles.class,
            methods =
            @RedirectInfos.MethodInfo(invokeType = invokeStatic, name = "privateLookupIn", methodReturnType = MethodHandles.Lookup.class, methodParameters = {Class.class, MethodHandles.Lookup.class})
    ))
    public static void hookPrivateLookupIn(MethodInvokeStack stack) throws Throwable {
        Class<?> c = (Class<?>) stack.getAsObject(0);
        if (c == null) return;
        checkClassAccess(stack, stack.getAsObject(1), c, newJLIllegalAccessException);
    }
}
