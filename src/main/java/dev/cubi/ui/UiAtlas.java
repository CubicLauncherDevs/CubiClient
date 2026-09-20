package dev.cubi.ui;

import dev.cubi.bridge.Game189;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import javax.imageio.ImageIO;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

/** One texture, two font weights and shared rounded corners. No runtime font rasterization. */
final class UiAtlas {
    private final Game189 game;
    private final int texture;
    private final float[][] advances = new float[2][224];
    private final float[] top = new float[2];

    UiAtlas(Game189 game) throws Throwable {
        this.game = game;
        try (DataInputStream stream = new DataInputStream(resource("atlas.bin"))) {
            if (stream.readInt() != 0x43554249) throw new IOException("Invalid Cubi atlas");
            for (int face = 0; face < 2; face++) {
                top[face] = stream.readFloat();
                for (int i = 0; i < 224; i++) advances[face][i] = stream.readFloat();
            }
        }
        BufferedImage image;
        try (InputStream stream = resource("atlas.png")) { image = ImageIO.read(stream); }
        if (image == null || image.getWidth() != 2048 || image.getHeight() != 1024) throw new IOException("Invalid atlas dimensions");
        ByteBuffer pixels = BufferUtils.createByteBuffer(2048 * 1024 * 4);
        for (int y = 0; y < 1024; y++) for (int x = 0; x < 2048; x++) {
            int pixel = image.getRGB(x, y);
            pixels.put((byte) (pixel >> 16)).put((byte) (pixel >> 8)).put((byte) pixel).put((byte) (pixel >>> 24));
        }
        pixels.flip();
        texture = GL11.glGenTextures();
        game.bindTexture(texture);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, 2048, 1024, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);
        game.bindTexture(0);
    }

    private static InputStream resource(String name) throws IOException {
        InputStream stream = UiAtlas.class.getResourceAsStream("/assets/cubi/ui/" + name);
        if (stream == null) throw new IOException("Missing Cubi UI asset: " + name);
        return stream;
    }

    float width(String value, float size, boolean bold) {
        float result = 0;
        for (int i = 0; i < value.length(); i++) result += advances[bold ? 1 : 0][glyph(value.charAt(i)) - 32];
        return result * size / 36;
    }

    void text(String value, float x, float y, float size, boolean bold, int color) throws Throwable {
        text(value, x, y, size, bold, color, 0);
    }

    void text(String value, float x, float y, float size, boolean bold, int color, float tracking) throws Throwable {
        int face = bold ? 1 : 0;
        float scale = size / 36;
        game.textureInk(texture, color);
        GL11.glBegin(GL11.GL_QUADS);
        try {
            for (int i = 0; i < value.length(); i++) {
                int glyph = glyph(value.charAt(i));
                int cell = face * 224 + glyph - 32;
                quad(x - 4 * scale, y - top[face] * scale, 64 * scale, 64 * scale,
                        cell % 32 * 64, cell / 32 * 64, 64, 64);
                x += advances[face][glyph - 32] * scale + tracking;
            }
        } finally { GL11.glEnd(); game.finishInk(); }
    }

    void rounded(float x, float y, float width, float height, float radius, int color) throws Throwable {
        patch(x, y, width, height, radius, color, 0);
    }

    void border(float x, float y, float width, float height, float radius, int color) throws Throwable {
        patch(x, y, width, height, radius, color, 384);
    }

    private void patch(float x, float y, float width, float height, float radius, int color, int sourceX) throws Throwable {
        float r = Math.min(radius, Math.min(width, height) / 2);
        if (width <= 0 || height <= 0 || (color >>> 24) == 0) return;
        game.textureInk(texture, color);
        GL11.glBegin(GL11.GL_QUADS);
        try {
            for (int row = 0; row < 3; row++) for (int col = 0; col < 3; col++) {
                float dx = col == 0 ? x : col == 1 ? x + r : x + width - r;
                float dy = row == 0 ? y : row == 1 ? y + r : y + height - r;
                float w = col == 1 ? width - 2 * r : r, h = row == 1 ? height - 2 * r : r;
                quad(dx, dy, w, h, sourceX + (col == 0 ? 0 : col == 1 ? 16 : 48),
                        960 + (row == 0 ? 0 : row == 1 ? 16 : 48), col == 1 ? 32 : 16, row == 1 ? 32 : 16);
            }
        } finally { GL11.glEnd(); game.finishInk(); }
    }

    void icon(int icon, float x, float y, float size, int color) throws Throwable {
        game.textureInk(texture, color);
        GL11.glBegin(GL11.GL_QUADS);
        try { quad(x, y, size, size, (icon + 1) * 64, 960, 64, 64); }
        finally { GL11.glEnd(); game.finishInk(); }
    }

    private static int glyph(char c) { return c >= 32 && c < 256 ? c : '?'; }
    private static void quad(float x, float y, float w, float h, float u, float v, float uw, float vh) {
        float a = u / 2048, b = v / 1024, c = (u + uw) / 2048, d = (v + vh) / 1024;
        GL11.glTexCoord2f(a, b); GL11.glVertex2f(x, y);
        GL11.glTexCoord2f(a, d); GL11.glVertex2f(x, y + h);
        GL11.glTexCoord2f(c, d); GL11.glVertex2f(x + w, y + h);
        GL11.glTexCoord2f(c, b); GL11.glVertex2f(x + w, y);
    }
}
