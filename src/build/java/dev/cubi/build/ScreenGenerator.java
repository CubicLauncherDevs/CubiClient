package dev.cubi.build;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/** Build-time generation: the shipped screen needs neither ASM nor LaunchWrapper. */
public final class ScreenGenerator implements Opcodes {
    private ScreenGenerator() { }

    public static byte[] bytecode() {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        // Outside dev.cubi.* so the optional LaunchWrapper exclusion doesn't catch it.
        writer.visit(V1_8, ACC_PUBLIC | ACC_FINAL, "cubi/generated/ControlScreen", null, "axu", null);
        MethodVisitor init = writer.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        init.visitCode();
        init.visitVarInsn(ALOAD, 0);
        init.visitMethodInsn(INVOKESPECIAL, "axu", "<init>", "()V", false);
        init.visitInsn(RETURN);
        init.visitMaxs(0, 0);
        init.visitEnd();
        delegate(writer, "a", "(IIF)V", "draw", new int[] {ILOAD, ILOAD, FLOAD});
        delegate(writer, "a", "(III)V", "click", new int[] {ILOAD, ILOAD, ILOAD});
        delegate(writer, "b", "(III)V", "release", new int[] {ILOAD, ILOAD, ILOAD});
        delegate(writer, "a", "(CI)V", "type", new int[] {ILOAD, ILOAD});
        delegate(writer, "m", "()V", "closed", new int[0]);
        MethodVisitor pause = writer.visitMethod(ACC_PUBLIC, "d", "()Z", null, null);
        pause.visitCode();
        pause.visitInsn(ICONST_0);
        pause.visitInsn(IRETURN);
        pause.visitMaxs(0, 0);
        pause.visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static void delegate(ClassWriter writer, String name, String desc, String callback, int[] loads) {
        MethodVisitor method = writer.visitMethod(ACC_PUBLIC, name, desc, null, null);
        method.visitCode();
        for (int i = 0; i < loads.length; i++) method.visitVarInsn(loads[i], i + 1);
        method.visitMethodInsn(INVOKESTATIC, "dev/cubi/ui/ControlDeck", callback, desc, false);
        method.visitInsn(RETURN);
        method.visitMaxs(0, 0);
        method.visitEnd();
    }

}
