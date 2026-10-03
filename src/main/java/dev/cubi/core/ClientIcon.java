package dev.cubi.core;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import javax.imageio.ImageIO;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.Display;

/** Window images are resized at build time and loaded once on client startup. */
final class ClientIcon {
    private ClientIcon() { }

    static void apply() throws IOException {
        Display.setIcon(new ByteBuffer[] {load(16), load(32), load(128)});
    }

    private static ByteBuffer load(int size) throws IOException {
        String path = "/assets/cubi/ui/icon-" + size + ".png";
        BufferedImage image;
        try (InputStream stream = ClientIcon.class.getResourceAsStream(path)) {
            if (stream == null) throw new IOException("Missing client icon: " + path);
            image = ImageIO.read(stream);
        }
        if (image == null || image.getWidth() != size || image.getHeight() != size) {
            throw new IOException("Invalid client icon: " + path);
        }
        ByteBuffer pixels = BufferUtils.createByteBuffer(size * size * 4);
        for (int y = 0; y < size; y++) for (int x = 0; x < size; x++) {
            int pixel = image.getRGB(x, y);
            pixels.put((byte) (pixel >> 16)).put((byte) (pixel >> 8)).put((byte) pixel).put((byte) (pixel >>> 24));
        }
        pixels.flip();
        return pixels;
    }
}
