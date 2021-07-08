package tester;

import io.github.karlatemp.jhf.api.event.EventPriority;
import io.github.karlatemp.jhf.api.events.JavaLangReflectInvokeEvent;
import io.github.karlatemp.jhf.api.markers.ConstructorAccessorImpl;
import io.github.karlatemp.jhf.api.markers.FieldAccessorImpl;
import io.github.karlatemp.jhf.api.markers.MagicAccessorImpl;
import io.github.karlatemp.jhf.api.markers.MethodAccessorImpl;
import io.github.karlatemp.jhf.api.utils.FilterMembers;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;

import java.lang.reflect.Method;
import java.util.Arrays;

public class MainTester {
    private static void dump(Class<?> c) {
        System.out.println(c);
        {
            Class<?>[] interfaces = c.getInterfaces();
            if (interfaces.length != 0) {
                System.out.append("=> ").println(Arrays.toString(interfaces));
            }
        }
        String prefix = " `-  ";
        c = c.getSuperclass();
        while (c != null) {
            System.out.append(prefix).println(c);
            Class<?>[] interfaces = c.getInterfaces();
            if (interfaces.length != 0) {
                System.out.append(prefix).append("=> ").println(Arrays.toString(interfaces));
            }
            prefix = "  " + prefix;
            c = c.getSuperclass();
        }
    }

    private static void launch() throws Throwable {
        Thread.dumpStack();
        MixinEnvironment.getCurrentEnvironment().setOption(MixinEnvironment.Option.DEBUG_ALL, true);

        FilterMembers.registerMethodFilter(method -> method.getName().equals("notFound"));
        JavaLangReflectInvokeEvent.EVENT_LINE.register(EventPriority.NORMAL, event -> {
            if (event.target instanceof Method) {
                if (event.target.getDeclaringClass().getName().endsWith("EskPermissionDenied")) {
                    throw new IllegalAccessException("Permission Denied");
                }
            }
        });

        Mixins.addConfiguration("tester/mixin/main.json");

        dump(MagicAccessorImpl.class);
        dump(MethodAccessorImpl.class);
        dump(FieldAccessorImpl.class);
        dump(ConstructorAccessorImpl.class);
        System.out.println(new MagicAccessorImpl() {
        });
    }
}
