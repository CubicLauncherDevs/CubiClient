package dev.cubi.launch;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;

/** Exact vanilla 1.8.9 mappings. Fail loudly if an expected hook disappears. */
public final class CubiTransformer implements IClassTransformer, Opcodes {
    private static final String HOOKS = "dev/cubi/core/Hooks";
    public static final String[] TARGETS = {"ave", "avo", "aya", "bec", "beb", "bfk"};

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !(name.equals("ave") || name.equals("avo") || name.equals("aya")
                || name.equals("bec") || name.equals("beb") || name.equals("bfk"))) return bytes;
        final String target = name;
        final int[] matches = new int[17];
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
                final boolean homeDraw = target.equals("aya") && method.equals("a") && desc.equals("(IIF)V");
                final boolean homeClick = target.equals("aya") && method.equals("a") && desc.equals("(III)V");
                final boolean homeType = target.equals("aya") && method.equals("a") && desc.equals("(CI)V");
                final boolean frame = target.equals("ave") && method.equals("av") && desc.equals("()V");
                final boolean particlePass = target.equals("bec") && method.equals("a") && desc.equals("(Lpk;F)V");
                final boolean particle = target.equals("beb") && method.equals("a") && desc.equals("(Lbfd;Lpk;FFFFFF)V");
                final boolean cameraFrame = target.equals("bfk") && method.equals("a") && desc.equals("(FJ)V");
                final boolean cameraFov = target.equals("bfk") && method.equals("a") && desc.equals("(FZ)F");
                if (cameraFrame) matches[13]++;
                if (cameraFov) matches[14]++;
                if (tick) matches[0]++;
                if (key) matches[1]++;
                if (hud || homeDraw) matches[2]++;
                if (homeClick) matches[11]++;
                if (homeType) matches[12]++;
                if (frame) matches[4]++;
                if (particlePass) matches[9]++;
                if (particle) matches[10]++;
                return new MethodVisitor(ASM5, next) {
                    @Override
                    public void visitCode() {
                        super.visitCode();
                        if (cameraFrame) call("zoomFrame", "()V");
                        if (homeDraw || homeClick || homeType) {
                            super.visitVarInsn(ALOAD, 0);
                            super.visitVarInsn(ILOAD, 1);
                            super.visitVarInsn(ILOAD, 2);
                            if (!homeType) super.visitVarInsn(homeDraw ? FLOAD : ILOAD, 3);
                            call(homeDraw ? "homeDraw" : homeClick ? "homeClick" : "homeType",
                                    homeDraw ? "(Ljava/lang/Object;IIF)Z" : homeClick ? "(Ljava/lang/Object;III)Z" : "(Ljava/lang/Object;CI)Z");
                            Label vanilla = new Label();
                            super.visitJumpInsn(IFEQ, vanilla);
                            super.visitInsn(RETURN);
                            super.visitLabel(vanilla);
                            super.visitFrame(F_SAME, 0, null, 0, null);
                        }
                        if (key) call("key", "()V");
                        if (frame) call("frameBegin", "()V");
                        if (particlePass) call("particlesBegin", "()V");
                        if (particle) {
                            super.visitVarInsn(ALOAD, 0);
                            for (int i = 3; i <= 8; i++) super.visitVarInsn(FLOAD, i);
                            call("particleVisible", "(Ljava/lang/Object;FFFFFF)Z");
                            Label visible = new Label();
                            super.visitJumpInsn(IFNE, visible);
                            super.visitInsn(RETURN);
                            super.visitLabel(visible);
                            // Only original arguments are live here; no hierarchy lookup needed.
                            super.visitFrame(F_SAME, 0, null, 0, null);
                        }
                    }

                    @Override
                    public void visitMethodInsn(int opcode, String owner, String method, String descriptor, boolean itf) {
                        if (owner.equals(HOOKS)) throw new IllegalStateException("CubiClient: hooks ya presentes en " + target);
                        if (cameraFrame && opcode == INVOKEVIRTUAL && owner.equals("bew") && method.equals("c") && descriptor.equals("(FF)V")) {
                            // [player, yaw, pitch] -> scale both deltas, preserving the original call.
                            matches[15]++;
                            call("zoomMouse", "(F)F");
                            super.visitInsn(SWAP);
                            call("zoomMouse", "(F)F");
                            super.visitInsn(SWAP);
                        }
                        int stage = -1;
                        if (frame) {
                            if (owner.equals("ave") && method.equals("s") && descriptor.equals("()V")) stage = 0;
                            if (owner.equals("bfk") && method.equals("a") && descriptor.equals("(FJ)V")) stage = 1;
                            if (owner.equals("ave") && method.equals("h") && descriptor.equals("()V")) stage = 2;
                            if (owner.equals("org/lwjgl/opengl/Display") && method.equals("sync") && descriptor.equals("(I)V")) stage = 3;
                        }
                        if (stage >= 0) { matches[5 + stage]++; stage("stageBegin", stage); }
                        super.visitMethodInsn(opcode, owner, method, descriptor, itf);
                        if (stage >= 0) stage("stageEnd", stage);
                        if (tick && opcode == INVOKESTATIC && owner.equals("org/lwjgl/input/Mouse")
                                && method.equals("next") && descriptor.equals("()Z")) {
                            // Preserve Mouse.next()'s boolean for the game's original branch.
                            matches[3]++;
                            call("mouse", "(Z)Z");
                        }
                    }

                    @Override
                    public void visitInsn(int opcode) {
                        if (cameraFov && opcode == FRETURN) {
                            matches[16]++;
                            super.visitVarInsn(ILOAD, 2);
                            call("zoomFov", "(FZ)F");
                        }
                        if (opcode == RETURN) {
                            if (tick) call("tick", "()V");
                            if (hud) call("hud", "()V");
                            if (frame) call("frameEnd", "()V");
                            if (particlePass) call("particlesEnd", "()V");
                        }
                        super.visitInsn(opcode);
                    }

                    private void call(String method, String descriptor) {
                        super.visitMethodInsn(INVOKESTATIC, HOOKS, method, descriptor, false);
                    }
                    private void stage(String name, int stage) {
                        super.visitInsn(ICONST_0 + stage);
                        call(name, "(I)V");
                    }
                };
            }
        }, 0);
        boolean valid = target.equals("ave") ? matches[0] == 1 && matches[1] == 1 && matches[3] == 1
                && matches[4] == 1 && matches[5] == 1 && matches[6] == 1 && matches[7] == 1 && matches[8] == 1
                : target.equals("bfk") ? matches[13] == 1 && matches[14] == 1 && matches[15] == 2 && matches[16] == 2
                : target.equals("aya") ? matches[2] == 1 && matches[11] == 1 && matches[12] == 1
                : target.equals("bec") ? matches[9] == 1 : target.equals("beb") ? matches[10] == 1 : matches[2] == 1;
        if (!valid) throw new IllegalStateException("CubiClient: bytecode incompatible para " + target + "; usa vanilla 1.8.9");
        System.out.println("[Cubi] Hooks 1.8.9 instalados: " + name);
        return writer.toByteArray();
    }
}
