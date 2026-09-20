package dev.cubi.tests;

/** JVM verification of the shipping patched classes, without initialization or a GL window. */
public final class LinkageTest {
    private LinkageTest() { }
    public static void main(String[] args) throws Exception {
        ClassLoader loader = LinkageTest.class.getClassLoader();
        for (String name : new String[] {"ave", "avo", "aya", "bec", "beb", "cubi.generated.ControlScreen"}) {
            Class<?> type = Class.forName(name, false, loader);
            if (!type.getProtectionDomain().getCodeSource().getLocation().toURI().equals(new java.io.File(args[0]).toURI())) {
                throw new AssertionError("Not loaded from replacement: " + name);
            }
            type.getDeclaredMethods(); // Forces verification of method bodies/stack map frames.
        }
        try {
            Class.forName("org.objectweb.asm.ClassReader", false, loader);
            throw new AssertionError("ASM leaked into runtime verification classpath");
        } catch (ClassNotFoundException expected) { /* The replacement must link without ASM. */ }
        System.out.println("PASS / JVM -Xverify:all: cinco clases parcheadas y pantalla enlazadas sin inicializar Minecraft ni OpenGL.");
    }
}
