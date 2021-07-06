package io.github.karlatemp.jhf.core.redirect;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RedirectInfos {
    public Info[] value();

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Info {
        Class<?> value() default void.class;

        String target() default "";

        MethodInfo[] methods();
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface MethodInfo {
        String name();

        InvokeType invokeType();

        String methodDesc() default "";

        Class<?> methodReturnType() default void.class;

        Class<?>[] methodParameters() default {};
    }
}
