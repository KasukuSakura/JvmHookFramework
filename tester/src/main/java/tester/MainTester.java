package tester;

import io.github.karlatemp.jhf.api.event.EventPriority;
import io.github.karlatemp.jhf.api.events.JavaLangReflectInvokeEvent;
import io.github.karlatemp.jhf.api.utils.FilterMembers;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;

import java.lang.reflect.Method;

public class MainTester {
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
    }
}
