package tester.mixin;

import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(targets = "testunit.RunTester")
@Debug
public class MixinRunTester {
    @Overwrite
    public static void error() {
    }
}
