package dev.cubi.ui;

import dev.cubi.core.ClientIdentity;
import dev.cubi.core.CubiClient;
import dev.cubi.ui.DeckLayout.Home;
import dev.cubi.ui.DeckLayout.Rect;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

/** Classic Minecraft home with a compact Cubi identity and one clear customization entry. */
public final class HomeScreen {
    private final CubiClient client;
    private final HomeState state = new HomeState();
    private final boolean[] enabled = new boolean[Home.COUNT];
    private final Motion[] hover = new Motion[Home.COUNT];
    private final int[] vanillaIds = {1, 2, -1, 0, 4, 5, 14};
    private Home layout;
    private Object parent;
    private int width = -1, height = -1;
    private boolean demo;

    public HomeScreen(CubiClient client) {
        this.client = client;
        for (int i = 0; i < hover.length; i++) hover[i] = new Motion();
    }

    private void prepare(Object owner) throws Throwable {
        parent = owner;
        state.bind(owner);
        client.game.resize();
        if (width != client.game.width || height != client.game.height) {
            width = client.game.width; height = client.game.height;
            layout = new Home(width, height);
        }
        demo = client.game.demo();
        vanillaIds[Home.PLAY] = demo ? 11 : 1;
        vanillaIds[Home.SERVERS] = demo ? 12 : 2;
        for (int i = 0; i < enabled.length; i++) {
            enabled[i] = vanillaIds[i] < 0 || client.game.menuEnabled(owner, vanillaIds[i]);
        }
    }

    public void draw(Object owner, int mouseX, int mouseY, float partial) throws Throwable {
        prepare(owner);
        Ink ink = client.ink;
        int hit = layout.hit(mouseX, mouseY);
        client.game.menuPanorama(owner, mouseX, mouseY, partial);
        ink.rect(0, 0, width, height, Theme.HOME_OVERLAY);
        GL11.glPushMatrix();
        try {
            GL11.glScalef(layout.scale, layout.scale, 1);
            ink.minecraftTitle(owner, layout.title.x, layout.title.y);
            int brandX = (layout.width - 22 - client.game.textWidth(ClientIdentity.NAME)) / 2;
            ink.logo(brandX, layout.brandY, 16);
            ink.minecraftText(ClientIdentity.NAME, brandX + 22, layout.brandY + 4, Theme.HOME_TEXT);

            button(Home.PLAY, demo ? "Jugar demo" : "Un jugador", hit, false);
            button(Home.SERVERS, demo ? "Restablecer demo" : "Multijugador", hit, false);
            button(Home.CUSTOMIZE, "Personalizar HUD", hit, false);
            button(Home.OPTIONS, "Opciones...", hit, false);
            button(Home.QUIT, "Salir", hit, false);
            button(Home.LANGUAGE, "Idioma", hit, true);
            button(Home.REALMS, "Realms", hit, true);

            String version = "Minecraft " + ClientIdentity.MINECRAFT + (demo ? " Demo" : "");
            ink.minecraftText(version, 6, layout.height - 14, Theme.TEXT);
            String clientVersion = ClientIdentity.NAME + " 0.0.1";
            ink.minecraftText(clientVersion, layout.width - 6 - client.game.textWidth(clientVersion), layout.height - 14, Theme.TEXT);
        } finally {
            GL11.glPopMatrix();
            ink.opacity = 1;
            client.game.finishInk();
        }
    }

    private float highlight(int control, int hit) {
        return hover[control].to(enabled[control] && (hit == control || state.focus() == control) ? 1 : 0);
    }
    private void button(int control, String title, int hit, boolean link) throws Throwable {
        Rect r = layout.controls[control];
        float amount = highlight(control, hit);
        Ink ink = client.ink;
        if (!link) {
            ink.rect(r.x + 1, r.y + 2, r.w, r.h, Theme.WINDOW_SHADOW);
            ink.rect(r.x, r.y, r.w, r.h, Ink.mix(Theme.HOME_BUTTON, Theme.HOME_BUTTON_HOVER, amount));
            ink.outline(r.x, r.y, r.w, r.h, Ink.mix(Theme.HOME_EDGE, Theme.ACCENT, amount));
        } else if (amount > 0.01f) {
            ink.rect(r.x, r.y, r.w, r.h, Ink.alpha(Theme.BACKGROUND, amount * 0.35f));
        }
        if (enabled[control] && state.focus() == control) ink.outline(r.x - 2, r.y - 2, r.w + 4, r.h + 4, Theme.ACCENT);
        int color = enabled[control] ? Theme.HOME_TEXT : Theme.MUTED;
        ink.minecraftText(title, r.x + (r.w - client.game.textWidth(title)) / 2f, r.y + (r.h - 8) / 2f, color);
    }

    public void click(Object owner, int x, int y, int button) throws Throwable {
        prepare(owner);
        if (button != 0) return;
        state.clearFocus();
        state.request(layout.hit(x, y), enabled);
    }
    public void type(Object owner, int key) throws Throwable {
        prepare(owner);
        if (Keyboard.isRepeatEvent() || key == client.config.menuKey) return;
        if (key == Keyboard.KEY_TAB) {
            state.move(Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT) ? -1 : 1, enabled);
        } else if (key == Keyboard.KEY_DOWN || key == Keyboard.KEY_RIGHT) state.move(1, enabled);
        else if (key == Keyboard.KEY_UP || key == Keyboard.KEY_LEFT) state.move(-1, enabled);
        else if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER || key == Keyboard.KEY_SPACE) state.request(state.focus(), enabled);
        else if (key == Keyboard.KEY_ESCAPE) state.clearFocus();
    }
    public void tick() throws Throwable {
        int control = state.take(client.game.screen());
        if (control < 0) return;
        if (control == Home.CUSTOMIZE) client.open();
        else client.game.menuAction(parent, vanillaIds[control]);
    }
}
