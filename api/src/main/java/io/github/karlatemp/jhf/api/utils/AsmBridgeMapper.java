package io.github.karlatemp.jhf.api.utils;

import org.objectweb.asm.commons.Remapper;
import org.spongepowered.asm.mixin.extensibility.IRemapper;

public class AsmBridgeMapper implements IRemapper {
    private final Remapper remapper;

    public AsmBridgeMapper(Remapper remapper) {
        this.remapper = remapper;
    }

    @Override
    public String mapMethodName(String owner, String name, String desc) {
        return remapper.mapMethodName(owner, name, desc);
    }

    @Override
    public String mapFieldName(String owner, String name, String desc) {
        return remapper.mapMethodName(owner, name, desc);
    }

    @Override
    public String map(String typeName) {
        return remapper.map(typeName);
    }

    @Override
    public String unmap(String typeName) {
        return null;
    }

    @Override
    public String mapDesc(String desc) {
        return null;
    }

    @Override
    public String unmapDesc(String desc) {
        return null;
    }
}
