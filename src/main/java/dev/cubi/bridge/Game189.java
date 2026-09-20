package dev.cubi.bridge;

import java.io.File;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/** All obfuscated access lives here. Method handles are resolved once, never per frame. */
public final class Game189 {
    private static ClassLoader runtimeLoader = Game189.class.getClassLoader();
    private final Object minecraft;
    private final Object settings;
    private final Object font;
    private final Field screen, world, hidden, debug;
    private final Object[] movement = new Object[5];
    private final MethodHandle keyCode, drawText, textWidth, rectangle, color;
    private final MethodHandle displayScreen, getFps, scaledWidth, scaledHeight;
    private final MethodHandle bindTexture, enableTexture, enableBlend, disableBlend, enableAlpha, disableAlpha, blendFunction;
    private final Constructor<?> scaledResolution;
    public final File directory;
    public int width = 854, height = 480;

    public Game189() throws Throwable {
        Class<?> mc = type("ave"), gui = type("axu"), resolution = type("avr");
        minecraft = mc.getDeclaredMethod("A").invoke(null);
        font = field(mc, "k").get(minecraft);
        settings = field(mc, "t").get(minecraft);
        directory = (File) field(mc, "v").get(minecraft);
        screen = field(mc, "m");
        world = field(mc, "f");
        hidden = field(type("avh"), "aA");
        debug = field(type("avh"), "aC");
        String[] keys = {"Y", "Z", "aa", "ab", "ac"};
        for (int i = 0; i < keys.length; i++) movement[i] = field(type("avh"), keys[i]).get(settings);
        keyCode = virtual(type("avb"), "i", int.class);
        drawText = virtual(type("avn"), "a", int.class, String.class, float.class, float.class, int.class);
        textWidth = virtual(type("avn"), "a", int.class, String.class);
        rectangle = handle(type("avp"), "a", int.class, int.class, int.class, int.class, int.class);
        color = handle(type("bfl"), "c", float.class, float.class, float.class, float.class);
        bindTexture = handle(type("bfl"), "i", int.class);
        enableTexture = handle(type("bfl"), "w");
        enableBlend = handle(type("bfl"), "l");
        disableBlend = handle(type("bfl"), "k");
        enableAlpha = handle(type("bfl"), "d");
        disableAlpha = handle(type("bfl"), "c");
        blendFunction = handle(type("bfl"), "a", int.class, int.class, int.class, int.class);
        displayScreen = handle(mc, "a", gui).asType(MethodType.methodType(void.class, Object.class, Object.class));
        getFps = handle(mc, "ai");
        scaledResolution = resolution.getConstructor(mc);
        scaledWidth = virtual(resolution, "a", int.class);
        scaledHeight = virtual(resolution, "b", int.class);
        resize();
    }

    public static Class<?> type(String name) throws ClassNotFoundException {
        return Class.forName(name, true, runtimeLoader);
    }

    /** The normal game loader is the default; the optional tweaker supplies its child loader. */
    public static void useClassLoader(ClassLoader loader) {
        if (loader == null) throw new IllegalArgumentException("Minecraft classloader is null");
        runtimeLoader = loader;
    }

    private static Field field(Class<?> owner, String name) throws NoSuchFieldException {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static MethodHandle handle(Class<?> owner, String name, Class<?>... arguments) throws IllegalAccessException, NoSuchMethodException {
        Method method = owner.getDeclaredMethod(name, arguments);
        method.setAccessible(true);
        return MethodHandles.lookup().unreflect(method);
    }

    private static MethodHandle virtual(Class<?> owner, String name, Class<?> result, Class<?>... arguments) throws IllegalAccessException, NoSuchMethodException {
        Class<?>[] adapted = new Class<?>[arguments.length + 1];
        adapted[0] = Object.class;
        System.arraycopy(arguments, 0, adapted, 1, arguments.length);
        return handle(owner, name, arguments).asType(MethodType.methodType(result, adapted));
    }

    public void resize() throws Throwable {
        Object scaled = scaledResolution.newInstance(minecraft);
        width = (int) scaledWidth.invokeExact(scaled);
        height = (int) scaledHeight.invokeExact(scaled);
    }

    public Object screen() throws IllegalAccessException { return screen.get(minecraft); }
    public boolean inWorld() throws IllegalAccessException { return world.get(minecraft) != null; }
    public boolean hudVisible() throws IllegalAccessException { return !hidden.getBoolean(settings) && !debug.getBoolean(settings); }
    public int fps() throws Throwable { return (int) getFps.invokeExact(); }
    public void show(Object next) throws Throwable { displayScreen.invokeExact(minecraft, next); }
    public boolean movementDown(int index) throws Throwable {
        int code = (int) keyCode.invokeExact(movement[index]);
        return code < 0 ? code >= -100 && code + 100 < Mouse.getButtonCount() && Mouse.isButtonDown(code + 100)
                : code > 0 && code < Keyboard.KEYBOARD_SIZE && Keyboard.isKeyDown(code);
    }
    public String movementName(int index) throws Throwable {
        int code = (int) keyCode.invokeExact(movement[index]);
        String name = code < 0 ? "M" + (code + 101) : Keyboard.getKeyName(code);
        if (index == 4 && code == Keyboard.KEY_SPACE) return "SPACE";
        return name == null ? "?" : name;
    }
    public void text(String text, float x, float y, int argb) throws Throwable {
        int ignored = (int) drawText.invokeExact(font, text, x, y, argb);
    }
    public int textWidth(String text) throws Throwable { return (int) textWidth.invokeExact(font, text); }
    public void rect(int x, int y, int right, int bottom, int argb) throws Throwable {
        rectangle.invokeExact(x, y, right, bottom, argb);
    }
    public void white() throws Throwable { color.invokeExact(1.0f, 1.0f, 1.0f, 1.0f); }
    public void bindTexture(int texture) throws Throwable { bindTexture.invokeExact(texture); }
    public void textureInk(int texture, int argb) throws Throwable {
        enableTexture.invokeExact(); enableBlend.invokeExact(); disableAlpha.invokeExact();
        blendFunction.invokeExact(770, 771, 1, 0);
        bindTexture.invokeExact(texture);
        color.invokeExact(((argb >> 16) & 255) / 255f, ((argb >> 8) & 255) / 255f,
                (argb & 255) / 255f, ((argb >>> 24) & 255) / 255f);
    }
    public void finishInk() throws Throwable {
        enableAlpha.invokeExact(); disableBlend.invokeExact(); white();
    }
}
