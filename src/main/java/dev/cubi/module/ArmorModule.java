package dev.cubi.module;

import dev.cubi.core.CubiClient;
import dev.cubi.config.ClientConfig;
import dev.cubi.ui.Ink;
import dev.cubi.ui.Theme;

public final class ArmorModule extends HudModule {
    private final Object[] items = new Object[4];
    private final int[] percent = {HudValues.EMPTY, HudValues.EMPTY, HudValues.EMPTY, HudValues.EMPTY};
    private final String[] labels = {"--", "--", "--", "--"};
    private static final int[] EXAMPLE = {100, 75, 40, 15};
    private static final String[] EXAMPLE_LABELS = {"100%", "75%", "40%", "15%"};
    public ArmorModule(ClientConfig config) {
        super(config, "armor", "Armor Status", "Equipo y durabilidad restante", "04", 64, 88);
        if (state.styleRevision < 1) {
            // Replace the old default once, keeping individually chosen opacity and controls.
            if (newState || state.opacity == 0.48f) state.opacity = 0;
            state.styleRevision = 1;
            config.changed();
        }
    }
    @Override public void tick(CubiClient client) throws Throwable {
        client.game.armor(items);
        for (int i = 0; i < 4; i++) {
            int value = client.game.durability(items[i]);
            if (value != percent[i]) { percent[i] = value; labels[i] = HudValues.durabilityLabel(value); }
        }
    }
    @Override public void paint(CubiClient client, Ink ink, boolean preview) throws Throwable {
        plate(ink);
        // Finish atlas geometry before invoking vanilla's item renderer (also with HUD batching on).
        for (int i = 0; i < 4; i++) {
            Object stack = preview ? client.game.previewArmor(i) : items[i];
            if (stack != null) ink.item(stack, 4, 6 + i * 20);
            else ink.icon(11 + i, 4, 6 + i * 20, 16, Theme.MUTED);
        }
        for (int i = 0; i < 4; i++) {
            int value = preview ? EXAMPLE[i] : percent[i];
            String label = preview ? EXAMPLE_LABELS[i] : labels[i];
            int color = value < 0 ? Theme.SECONDARY : value <= 20 ? Theme.DANGER : value <= 50 ? Theme.WARNING : Theme.SUCCESS;
            fittedText(ink, label, 26, 10 + i * 20, width - 30, 9, color);
        }
    }
    @Override public int icon() { return 8; }
    @Override public void clearData() {
        for (int i = 0; i < 4; i++) { items[i] = null; percent[i] = HudValues.EMPTY; labels[i] = "--"; }
    }
}
