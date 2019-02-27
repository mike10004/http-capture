package io.github.mike10004.debexam;

import com.google.common.io.BaseEncoding;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static java.util.Objects.requireNonNull;

public class DpkgExtraction implements DebModel {

    private final String versionHex;
    private final String versionAscii;
    private final DebControl control;
    private final DebData data;

    public DpkgExtraction(String versionHex, DebControl control, DebData data) {
        this(versionHex, null, control, data);
    }

    private DpkgExtraction(String versionHex, String versionAscii, DebControl control, DebData data) {
        this.versionHex = requireNonNull(versionHex);
        this.versionAscii = versionAscii;
        this.control = requireNonNull(control);
        this.data = requireNonNull(data);
    }

    public static DpkgExtraction onDisk(String versionAscii, Path extractedControlRoot, Path extractedDataRoot) throws IOException {
        VirtualFilesystem controlFs = new DiskFilesystem(extractedControlRoot).freezeIndex();
        VirtualFilesystem dataFs = new DiskFilesystem(extractedDataRoot).freezeIndex();
        byte[] versionBytes = versionAscii.getBytes(StandardCharsets.UTF_8);
        String versionHex = BaseEncoding.base16().encode(versionBytes);
        return new DpkgExtraction(versionHex, versionAscii, DebControl.predefined(controlFs), DebData.predefined(dataFs));
    }

    @Override
    public String getVersionAscii() {
        if (versionAscii != null) {
            return versionAscii;
        }
        return DebModel.super.getVersionAscii();
    }

    @Override
    public String getVersionHex() {
        return versionHex;
    }

    @Override
    public DebControl getControl() {
        return control;
    }

    @Override
    public DebData getData() {
        return data;
    }
}
