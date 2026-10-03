package dev.cubi.camera;

import dev.cubi.config.ClientConfig;

/** Persistent camera preferences, independent of vanilla options.txt. */
public final class ZoomSettings {
    public boolean enabled = true;
    public int key = 46; // LWJGL C; zero means unassigned.
    public float magnification = 4;
    public boolean smooth = true;
    public boolean adaptSensitivity = true;

    public void sanitize() {
        if (key < 0 || key == 1 || key > 255) key = 46;
        magnification = ClientConfig.finiteClamp(magnification, 2, 8, 4);
    }
}
