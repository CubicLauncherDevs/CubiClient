package dev.cubi.build;

import dev.cubi.launch.CubiTransformer;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.util.CheckClassAdapter;

/** Shares the exact transformer with the library build, but applies it before packaging. */
public final class PrepareClasses {
    private PrepareClasses() { }

    public static void main(String[] arguments) throws Exception {
        if (arguments.length != 1 && arguments.length != 3) {
            throw new IllegalArgumentException("classes-directory [vanilla-jar patched-directory]");
        }
        writeChecked(Paths.get(arguments[0]).resolve("cubi/generated/ControlScreen.class"), ScreenGenerator.bytecode());
        if (arguments.length == 3) {
            CubiTransformer transformer = new CubiTransformer();
            Path patched = Paths.get(arguments[2]);
            try (ZipFile original = new ZipFile(arguments[1])) {
                for (String name : CubiTransformer.TARGETS) {
                    ZipEntry entry = original.getEntry(name + ".class");
                    if (entry == null) throw new IllegalArgumentException("Missing vanilla class " + name);
                    byte[] bytes;
                    try (InputStream stream = original.getInputStream(entry); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                        byte[] buffer = new byte[8192];
                        int size;
                        while ((size = stream.read(buffer)) != -1) out.write(buffer, 0, size);
                        bytes = out.toByteArray();
                    }
                    writeChecked(patched.resolve(name + ".class"), transformer.transform(name, name, bytes));
                }
            }
        }
    }

    private static void writeChecked(Path file, byte[] bytes) throws Exception {
        new ClassReader(bytes).accept(new CheckClassAdapter(new ClassWriter(0), true), 0);
        Files.createDirectories(file.getParent());
        Files.write(file, bytes);
    }
}
