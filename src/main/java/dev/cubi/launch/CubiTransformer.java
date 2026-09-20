package dev.cubi.launch;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/** Exact vanilla 1.8.9 mappings. Fail loudly if an expected hook disappears. */
public final class CubiTransformer implements IClassTransformer, Opcodes {
    private static final String HOOKS = "dev/cubi/core/Hooks";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !(name.equals("ave") || name.equals("avo") || name.equals("aya"))) return bytes;
        final String target = name;
        final int[] matches = new int[4];
        ClassReader reader = new ClassReader(bytes);
        // Hooks preserve stack shapes; existing frames remain valid without hierarchy loading.
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        reader.accept(new ClassVisitor(ASM5, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String method, String desc, String signature, String[] exceptions) {
                MethodVisitor next = super.visitMethod(access, method, desc, signature, exceptions);
                final boolean tick = target.equals("ave") && method.equals("s") && desc.equals("()V");
                final boolean key = target.equals("ave") && method.equals("Z") && desc.equals("()V");
                final boolean hud = target.equals("avo") && method.equals("a") && desc.equals("(F)V");
                final boolean brand = target.equals("aya") && method.equals("a") && desc.equals("(IIF)V");
                if (tick) matches[0]++;
                if (key) matches[1]++;
                if (hud || brand) matches[2]++;
                return new MethodVisitor(ASM5, next) {
                    @Override
                    public void visitCode() {
                        super.visitCode();
                        if (key) call("key", "()V");
                    }

                    @Override
                    public void visitMethodInsn(int opcode, String owner, String method, String descriptor, boolean itf) {
                        super.visitMethodInsn(opcode, owner, method, descriptor, itf);
                        if (tick && opcode == INVOKESTATIC && owner.equals("org/lwjgl/input/Mouse")
                                && method.equals("next") && descriptor.equals("()Z")) {
                            // Preserve Mouse.next()'s boolean for the game's original branch.
                            matches[3]++;
                            call("mouse", "(Z)Z");
                        }
                    }

                    @Override
                    public void visitInsn(int opcode) {
                        if (opcode == RETURN) {
                            if (tick) call("tick", "()V");
                            if (hud) call("hud", "()V");
                            if (brand) call("brand", "()V");
                        }
                        super.visitInsn(opcode);
                    }

                    private void call(String method, String descriptor) {
                        super.visitMethodInsn(INVOKESTATIC, HOOKS, method, descriptor, false);
                    }
                };
            }
        }, 0);
        boolean valid = target.equals("ave") ? matches[0] == 1 && matches[1] == 1 && matches[3] == 1 : matches[2] == 1;
        if (!valid) throw new IllegalStateException("CubiClient: bytecode incompatible para " + target + "; usa vanilla 1.8.9");
        System.out.println("[Cubi] Hooks 1.8.9 instalados: " + name);
        return writer.toByteArray();
    }
}
