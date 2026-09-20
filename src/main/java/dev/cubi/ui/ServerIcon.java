package dev.cubi.ui;

import dev.cubi.bridge.Game189;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

/** One 64x64 favicon, decoded/uploaded only when server metadata changes on the game tick. */
public final class ServerIcon {
    private final Game189 game;
    private String source;
    private int texture;

    public ServerIcon(Game189 game) { this.game = game; }
    public int texture() { return texture; }

    public void update(String encoded) throws Throwable {
        if (encoded == null ? source == null : encoded.equals(source)) return;
        clear();
        source = encoded;
        if (encoded == null || encoded.isEmpty() || encoded.length() > 65536) return;
        BufferedImage image;
        try { image = decode(encoded); }
        catch (IOException | IllegalArgumentException invalidIcon) { return; }
        if (image == null) return;
        ByteBuffer pixels = BufferUtils.createByteBuffer(64 * 64 * 4);
        for (int y = 0; y < 64; y++) for (int x = 0; x < 64; x++) {
            int pixel = image.getRGB(x, y);
            pixels.put((byte) (pixel >> 16)).put((byte) (pixel >> 8)).put((byte) pixel).put((byte) (pixel >>> 24));
        }
        pixels.flip();
        texture = GL11.glGenTextures();
        try {
            game.bindTexture(texture);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, 64, 64, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);
        } finally { game.bindTexture(0); }
    }

    public void clear() throws Throwable {
        if (texture != 0) { game.deleteTexture(texture); texture = 0; }
        source = null;
    }

    private static BufferedImage decode(String encoded) throws IOException {
        String prefix = "data:image/png;base64,";
        byte[] bytes = Base64.getDecoder().decode(encoded.startsWith(prefix) ? encoded.substring(prefix.length()) : encoded);
        try (ImageInputStream input = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) return null;
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                if (!reader.getFormatName().equalsIgnoreCase("png") || reader.getWidth(0) != 64 || reader.getHeight(0) != 64) return null;
                return reader.read(0);
            } finally { reader.dispose(); }
        }
    }
}
