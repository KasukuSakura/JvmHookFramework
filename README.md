# Jvm Hook Framework

[简体中文](README.zh.md)

----

# Desc

`JVM Hook Framework` is designed for injecting java classes with friendly apis using `SpongePowered/mixin`

[![Maven Central](https://img.shields.io/maven-central/v/io.github.karlatemp.jhf/api.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:io.github.karlatemp.jhf)

# Using

You need add `Maven Central` and `https://repo.spongepowered.org/repository/maven-public/` into your repositories.

```groovy
plugins {
    id 'java-library'
}

repositories {
    mavenCentral()
    maven { url = 'https://repo.spongepowered.org/repository/maven-public/' }
    maven { url = 'https://files.minecraftforge.net/maven/' }
}

dependencies {
    api 'io.github.karlatemp.jhf:api:<version>'
}
```

## Developing a plugin

### Project setup

- Create a new class named `myjhfplugin.Startup` (or other you like)
- Create a new file in project resources named `jhf-main.txt`
- Write `myjhfplugin.Start` in `jhf-main.txt`
- Add a **static** method in `myjhfplugin.Startup` named **launch** with **no parameter** and **void return type**
  > `private static void launch() throws Throwable {}`

### Using mixin

You need create a class module of class you want to inject.

```java
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(targets = "testunit.RunTester")
public class MixinRunTester {
    @Overwrite
    public static void error() {
    }
}
```

Then, you need create a mixin configuration file in your project resources. Suggested path format
is `${project-name}/mixin/${config-name}.json`

```json5
{
  "minVersion": 0,
  "package": "tester.mixin",
  // The package storing mixin classes
  "mixins": [
    "MixinRunTester"
  ],
  // Class name that relative to ${package}
  "client": [],
  // no effect
  "server": []
  // no effect
}
```

In your `Plugin Main Point` add following code

```java
public class MainPlugin {
    private static void launch() throws Throwable {
        Mixins.addConfiguration("myplugin/mixin/main.json");
    }
}

```

> Example: [MixinRunTester](tester/src/main/java/tester/mixin/MixinRunTester.java)

## Listen `JavaLangReflectInvokeEvent`

`JavaLangReflectInvokeEvent` will broadcast when application invoking reflection of `java.lang.reflect`.

```java
JavaLangReflectInvokeEvent.EVENT_LINE.register(EventPriority.NORMAL,event-> {
    if (event.target instanceof Method) {
        if (event.target.getDeclaringClass().getName().endsWith("EskPermissionDenied")) {
            throw new IllegalAccessException("Permission Denied");
        }
    }
});
```

## Modify bytecodes

When a new class before loading, a `TransformBytecodeEvent` will broadcast to event-pipeline.

Listen and modify `TransformBytecodeEvent.bytecode` to change bytecode with the lowest way.

## Find an example

The folder `tester` is a plugin for `JvmHookFramework Testing`. You can refer to it to write your own plugin.

## Plugin configuration

JHF using `SpongePowered/Configuration` to manage configurations. Using it by

```java
public class Main {
    public static final File DATA_FOLDER = JvmHookFramework.getInstance().getDataFolder("my-plugin");
    public static final HoconConfigurationLoader LOADER = JvmHookFramework.getInstance().newConfigLoader(
            ConfigurationOptions.defaults()
                    .shouldCopyDefaults(true)
            ,
            new File(DATA_FOLDER).toPath()
    );
}
```

