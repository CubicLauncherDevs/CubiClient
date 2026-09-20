package dev.cubi.bridge;

import dev.cubi.performance.ParticleVisibility;
import dev.cubi.performance.ResolutionCache;
import dev.cubi.performance.VideoSettings;
import dev.cubi.module.HudValues;
import java.util.UUID;
import org.lwjgl.opengl.GL11;
import java.io.File;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

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
    private final MethodHandle bindTexture, deleteTexture, enableTexture, enableBlend, disableBlend, enableAlpha, disableAlpha, blendFunction;
    private final Constructor<?> scaledResolution;
    private final Field displayWidth, displayHeight, guiScale, renderGlobal;
    private final Field distance, frameLimit, clouds, particles, fancy, ao, vsync, vbo, shadows;
    private final MethodHandle unicode, reloadRenderers, saveOptions;
    private final ResolutionCache resolutionCache = new ResolutionCache();
    private ParticleAccess particleAccess;
    private final MethodHandle player, posX, posY, posZ, playerId, connection, playerInfo, responseTime;
    private final MethodHandle currentServer, singleplayer, serverAddress, serverIcon, inventory, armor;
    private final MethodHandle itemDamage, itemMaximum, itemDamageable, itemRenderer, renderItem;
    private final MethodHandle guiLighting, disableLighting, enableDepth, disableDepth, depthMask, disableRescale;
    private final MethodHandle itemById;
    private final Constructor<?> itemStack;
    private final Object[] previewArmor = new Object[4];
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
        deleteTexture = handle(type("bfl"), "h", int.class);
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
        displayWidth = field(mc, "d"); displayHeight = field(mc, "e");
        guiScale = field(type("avh"), "aL"); unicode = virtual(mc, "d", boolean.class);
        renderGlobal = field(mc, "g");
        reloadRenderers = virtual(type("bfr"), "a", void.class);
        saveOptions = virtual(type("avh"), "b", void.class);
        Class<?> options = type("avh");
        distance = field(options, "c"); frameLimit = field(options, "g"); clouds = field(options, "h");
        particles = field(options, "aM"); fancy = field(options, "i"); ao = field(options, "j");
        vsync = field(options, "t"); vbo = field(options, "u"); shadows = field(options, "W");
        player = getter(mc, "h", Object.class);
        posX = getter(type("pk"), "s", double.class); posY = getter(type("pk"), "t", double.class);
        posZ = getter(type("pk"), "u", double.class); playerId = virtual(type("pk"), "aK", UUID.class);
        connection = virtual(mc, "u", Object.class);
        playerInfo = virtual(type("bcy"), "a", Object.class, UUID.class);
        responseTime = virtual(type("bdc"), "c", int.class);
        currentServer = virtual(mc, "D", Object.class); singleplayer = virtual(mc, "E", boolean.class);
        serverAddress = getter(type("bde"), "b", String.class);
        serverIcon = virtual(type("bde"), "c", String.class);
        inventory = getter(type("wn"), "bi", Object.class); armor = getter(type("wm"), "b", Object[].class);
        Class<?> stack = type("zx");
        itemDamage = virtual(stack, "h", int.class); itemMaximum = virtual(stack, "j", int.class);
        itemDamageable = virtual(stack, "e", boolean.class);
        itemRenderer = virtual(mc, "ag", Object.class);
        renderItem = handle(type("bjh"), "b", stack, int.class, int.class)
                .asType(MethodType.methodType(void.class, Object.class, Object.class, int.class, int.class));
        guiLighting = handle(type("avc"), "c"); disableLighting = handle(type("avc"), "a");
        enableDepth = handle(type("bfl"), "j"); disableDepth = handle(type("bfl"), "i");
        depthMask = handle(type("bfl"), "a", boolean.class); disableRescale = handle(type("bfl"), "C");
        itemById = handle(type("zw"), "b", int.class).asType(MethodType.methodType(Object.class, int.class));
        itemStack = stack.getConstructor(type("zw"));
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
        int w = displayWidth.getInt(minecraft), h = displayHeight.getInt(minecraft), scale = guiScale.getInt(settings);
        boolean useUnicode = (boolean) unicode.invokeExact(minecraft);
        if (resolutionCache.matches(w, h, scale, useUnicode)) return;
        Object scaled = scaledResolution.newInstance(minecraft);
        width = (int) scaledWidth.invokeExact(scaled);
        height = (int) scaledHeight.invokeExact(scaled);
        resolutionCache.update(w, h, scale, useUnicode);
    }

    public VideoSettings videoSettings() throws IllegalAccessException {
        VideoSettings v = new VideoSettings();
        v.distance = distance.getInt(settings); v.fps = frameLimit.getInt(settings);
        v.clouds = clouds.getInt(settings); v.particles = particles.getInt(settings);
        v.fancy = fancy.getBoolean(settings); v.ambientOcclusion = ao.getInt(settings);
        v.vsync = vsync.getBoolean(settings); v.vbo = vbo.getBoolean(settings); v.shadows = shadows.getBoolean(settings);
        return v;
    }

    public boolean videoMatches(VideoSettings v) throws IllegalAccessException {
        return distance.getInt(settings) == v.distance && frameLimit.getInt(settings) == v.fps
                && clouds.getInt(settings) == v.clouds && particles.getInt(settings) == v.particles
                && fancy.getBoolean(settings) == v.fancy && ao.getInt(settings) == v.ambientOcclusion
                && vsync.getBoolean(settings) == v.vsync && vbo.getBoolean(settings) == v.vbo
                && shadows.getBoolean(settings) == v.shadows;
    }

    /** Apply all fields before a single terrain reload. Called only from a user action. */
    public void applyVideo(VideoSettings v) throws Throwable {
        v.sanitize();
        VideoSettings old = videoSettings();
        if (old.same(v)) return;
        distance.setInt(settings, v.distance); frameLimit.setInt(settings, v.fps);
        clouds.setInt(settings, v.clouds); particles.setInt(settings, v.particles);
        fancy.setBoolean(settings, v.fancy); ao.setInt(settings, v.ambientOcclusion);
        vsync.setBoolean(settings, v.vsync); vbo.setBoolean(settings, v.vbo); shadows.setBoolean(settings, v.shadows);
        if (old.vsync != v.vsync) Display.setVSyncEnabled(v.vsync);
        Object renderer = renderGlobal.get(minecraft);
        if (renderer != null && (old.distance != v.distance || old.fancy != v.fancy
                || old.ambientOcclusion != v.ambientOcclusion || old.vbo != v.vbo)) reloadRenderers.invokeExact(renderer);
        saveOptions.invokeExact(settings);
    }

    public Object worldIdentity() throws IllegalAccessException { return world.get(minecraft); }

    public Object player() throws Throwable { return inWorld() ? (Object) player.invokeExact(minecraft) : null; }
    public int ping() throws Throwable {
        Object p = player();
        if (p == null) return -1;
        Object net = (Object) connection.invokeExact(minecraft);
        if (net == null) return -1;
        Object info = (Object) playerInfo.invokeExact(net, (UUID) playerId.invokeExact(p));
        return info == null ? -1 : (int) responseTime.invokeExact(info);
    }
    public int blockX(Object p) throws Throwable { return HudValues.block((double) posX.invokeExact(p)); }
    public int blockY(Object p) throws Throwable { return HudValues.block((double) posY.invokeExact(p)); }
    public int blockZ(Object p) throws Throwable { return HudValues.block((double) posZ.invokeExact(p)); }
    public String server() throws Throwable {
        if (!inWorld()) return HudValues.server(false, false, null);
        boolean local = (boolean) singleplayer.invokeExact(minecraft);
        Object data = (Object) currentServer.invokeExact(minecraft);
        return HudValues.server(true, local, data == null ? null : (String) serverAddress.invokeExact(data));
    }
    /** Same cached favicon shown by the vanilla server list, without extra network requests. */
    public String serverIcon() throws Throwable {
        if (!inWorld() || (boolean) singleplayer.invokeExact(minecraft)) return null;
        Object data = (Object) currentServer.invokeExact(minecraft);
        return data == null ? null : (String) serverIcon.invokeExact(data);
    }
    /** Vanilla inventory order is boots to helmet; the HUD displays helmet to boots. */
    public void armor(Object[] target) throws Throwable {
        Object p = player();
        Object inv = p == null ? null : (Object) inventory.invokeExact(p);
        Object[] equipped = inv == null ? null : (Object[]) armor.invokeExact(inv);
        for (int i = 0; i < 4; i++) target[i] = equipped == null ? null : equipped[3 - i];
    }
    public int durability(Object stack) throws Throwable {
        if (stack == null) return HudValues.EMPTY;
        return HudValues.durability((int) itemMaximum.invokeExact(stack), (int) itemDamage.invokeExact(stack),
                (boolean) itemDamageable.invokeExact(stack));
    }
    public Object previewArmor(int slot) throws Throwable {
        if (previewArmor[slot] == null) previewArmor[slot] = itemStack.newInstance((Object) itemById.invokeExact(310 + slot));
        return previewArmor[slot];
    }
    /** Called only after flushing the UI atlas. Restore depth via the vanilla state cache. */
    public void item(Object stack, int x, int y) throws Throwable {
        if (stack == null) return;
        boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST), writeDepth = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        try {
            enableTexture.invokeExact(); enableDepth.invokeExact(); depthMask.invokeExact(true);
            guiLighting.invokeExact();
            renderItem.invokeExact((Object) itemRenderer.invokeExact(minecraft), stack, x, y);
        } finally {
            disableLighting.invokeExact(); disableRescale.invokeExact();
            if (depth) enableDepth.invokeExact(); else disableDepth.invokeExact();
            depthMask.invokeExact(writeDepth);
            finishInk();
        }
    }

    public void beginParticles() throws Throwable {
        if (particleAccess == null) particleAccess = new ParticleAccess();
        particleAccess.planes = null;
    }

    public boolean particleVisible(Object particle, float partial, float rx, float rxz, float rz, float ryz, float rxy) throws Throwable {
        return particleAccess == null || particleAccess.visible(particle, partial, rx, rxz, rz, ryz, rxy);
    }

    private static MethodHandle getter(Class<?> owner, String name, Class<?> result) throws Exception {
        return MethodHandles.lookup().unreflectGetter(field(owner, name)).asType(MethodType.methodType(result, Object.class));
    }

    /** Lazy and isolated: a culling binding failure must not disable the HUD. */
    private static final class ParticleAccess {
        private final Class<?> base = type("beb"), buffer = type("bfd"), entity = type("pk");
        private final MethodHandle px = getter(entity, "p", double.class), py = getter(entity, "q", double.class), pz = getter(entity, "r", double.class);
        private final MethodHandle x = getter(entity, "s", double.class), y = getter(entity, "t", double.class), z = getter(entity, "u", double.class);
        private final MethodHandle size = getter(base, "h", float.class);
        private final Field cameraX = field(base, "aw"), cameraY = field(base, "ax"), cameraZ = field(base, "ay");
        private final MethodHandle frustum = handle(type("bib"), "a").asType(MethodType.methodType(Object.class));
        private final Field frustumPlanes = field(type("bid"), "a");
        private float[][] planes;
        private double cx, cy, cz;
        private final ClassValue<Boolean> ordinary = new ClassValue<Boolean>() {
            @Override protected Boolean computeValue(Class<?> type) {
                try {
                    // Overridden renderers (including those calling super) retain vanilla rendering.
                    return type.getMethod("a", buffer, entity, float.class, float.class, float.class,
                            float.class, float.class, float.class).getDeclaringClass() == base;
                } catch (NoSuchMethodException error) { return false; }
            }
        };
        ParticleAccess() throws Exception { }

        boolean visible(Object p, float partial, float rx, float rxz, float rz, float ryz, float rxy) throws Throwable {
            if (!ordinary.get(p.getClass())) return true;
            if (planes == null) {
                Object clipping = (Object) frustum.invokeExact();
                planes = (float[][]) frustumPlanes.get(clipping);
                cx = cameraX.getDouble(null); cy = cameraY.getDouble(null); cz = cameraZ.getDouble(null);
            }
            return ParticleVisibility.visible(planes,
                    ParticleVisibility.center((double) px.invokeExact(p), (double) x.invokeExact(p), partial, cx),
                    ParticleVisibility.center((double) py.invokeExact(p), (double) y.invokeExact(p), partial, cy),
                    ParticleVisibility.center((double) pz.invokeExact(p), (double) z.invokeExact(p), partial, cz),
                    (float) size.invokeExact(p), rx, rxz, rz, ryz, rxy);
        }
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
    public void deleteTexture(int texture) throws Throwable { deleteTexture.invokeExact(texture); }
    public void textureInk(int texture, int argb) throws Throwable {
        enableTexture.invokeExact(); enableBlend.invokeExact(); disableAlpha.invokeExact();
        blendFunction.invokeExact(770, 771, 1, 0);
        bindTexture.invokeExact(texture);
        inkColor(argb);
    }
    public void inkColor(int argb) throws Throwable {
        color.invokeExact(((argb >> 16) & 255) / 255f, ((argb >> 8) & 255) / 255f,
                (argb & 255) / 255f, ((argb >>> 24) & 255) / 255f);
    }
    public void finishInk() throws Throwable {
        enableAlpha.invokeExact(); disableBlend.invokeExact(); white();
    }
}
