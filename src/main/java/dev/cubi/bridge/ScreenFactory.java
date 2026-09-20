package dev.cubi.bridge;

/** Instantiates the GuiScreen subclass prepared at build time. */
public final class ScreenFactory {
    private static Class<?> screenType;
    private ScreenFactory() { }

    public static Object create() throws ReflectiveOperationException {
        if (screenType == null) screenType = Game189.type("cubi.generated.ControlScreen");
        return screenType.getDeclaredConstructor().newInstance();
    }
}
