package io.github.karlatemp.jhf.launcher;

import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataOutput;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.List;

public class FlattenJarFile {
    public static class ModuleInfo {
        public String name;
        public List<String> packages;
        public byte[] manifest;

        public ModuleInfo initialize() {
            packages = new ArrayList<>();
            manifest = CSWHelper.EMPTY_BYTE_ARRAY;
            return this;
        }

        public void write(DataOutput output) throws Exception {
            output.writeUTF(name);
            CSWHelper.writeArray(output, manifest);
            CSWHelper.writeList(output, packages, (k, v) -> v.writeUTF(k));
        }

        public static ModuleInfo read(DataInput input) throws Exception {
            ModuleInfo mi = new ModuleInfo();
            mi.name = input.readUTF();
            mi.manifest = CSWHelper.readArray(input);
            mi.packages = CSWHelper.readList(input, DataInput::readUTF);
            return mi;
        }
    }

    public static class ResPair {
        public String name;
        public long pointer;
        public long size;
        public int[] signers;

        public ResPair initialize() {
            signers = CSWHelper.EMPTY_INT_ARRAY;
            return this;
        }

        public void write(DataOutput output) throws Exception {
            output.writeUTF(name);
            output.writeLong(pointer);
            output.writeLong(size);
            CSWHelper.writeArray(output, signers);
        }

        public static ResPair read(DataInput input) throws Exception {
            ResPair resp = new ResPair();
            resp.name = input.readUTF();
            resp.pointer = input.readLong();
            resp.size = input.readLong();
            resp.signers = CSWHelper.readIntArray(input);
            return resp;
        }
    }

    public List<ModuleInfo> modules;
    public List<ResPair> resources;
    public List<Certificate> certificates;
    public long imageSize;

    public FlattenJarFile initialize() {
        modules = new ArrayList<>();
        resources = new ArrayList<>();
        certificates = new ArrayList<>();
        return this;
    }

    public void write(DataOutput output) throws Exception {
        CSWHelper.writeList(output, modules, ModuleInfo::write);
        CSWHelper.writeList(output, resources, ResPair::write);
        CSWHelper.writeList(output, certificates, (certificate, outputx) -> {
            outputx.writeUTF(certificate.getType());
            CSWHelper.writeArray(outputx, certificate.getEncoded());
        });
        output.writeLong(imageSize);
    }

    public static FlattenJarFile read(DataInput input) throws Exception {
        FlattenJarFile resp = new FlattenJarFile();
        resp.modules = CSWHelper.readList(input, ModuleInfo::read);
        resp.resources = CSWHelper.readList(input, ResPair::read);
        resp.certificates = CSWHelper.readList(input, arg -> CertificateFactory.getInstance(arg.readUTF()).generateCertificate(
                new ByteArrayInputStream(CSWHelper.readArray(arg))
        ));
        resp.imageSize = input.readLong();
        return resp;
    }
}
