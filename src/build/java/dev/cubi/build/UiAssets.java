package dev.cubi.build;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.DataOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.imageio.ImageIO;

/** Rasterize once at build time. The game only uploads this shared atlas once. */
public final class UiAssets {
    private UiAssets() { }
    public static void main(String[] args) throws Exception {
        Path output = Paths.get(args[0]).resolve("assets/cubi/ui");
        Files.createDirectories(output);
        BufferedImage image = new BufferedImage(2048, 1024, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.setColor(Color.WHITE);
        try (DataOutputStream metrics = new DataOutputStream(Files.newOutputStream(output.resolve("atlas.bin")))) {
            metrics.writeInt(0x43554249);
            for (int face = 0; face < 2; face++) {
                Font font = Font.createFont(Font.TRUETYPE_FONT, Paths.get(args[face + 1]).toFile()).deriveFont(36f);
                if (!font.getFamily(java.util.Locale.ROOT).equals("Cantarell")) {
                    throw new IllegalArgumentException("Expected Cantarell, found " + font.getFontName());
                }
                g.setFont(font);
                FontMetrics fm = g.getFontMetrics();
                int baseline = 4 + fm.getAscent();
                metrics.writeFloat(baseline + font.createGlyphVector(g.getFontRenderContext(), "H").getPixelBounds(null, 0, 0).y);
                for (int code = 32; code < 256; code++) {
                    int cell = face * 224 + code - 32;
                    int x = cell % 32 * 64, y = cell / 32 * 64;
                    String glyph = Character.toString((char) code);
                    java.awt.Rectangle bounds = font.createGlyphVector(g.getFontRenderContext(), glyph).getPixelBounds(null, 4, baseline);
                    if (bounds.x < 0 || bounds.y < 0 || bounds.x + bounds.width > 64 || bounds.y + bounds.height > 64) {
                        throw new IllegalArgumentException("Glyph does not fit atlas cell: " + code + " / " + font.getFontName());
                    }
                    g.drawString(glyph, x + 4, y + baseline);
                    metrics.writeFloat((float) font.getStringBounds(glyph, g.getFontRenderContext()).getWidth());
                }
            }
        }
        g.fillRoundRect(0, 960, 64, 64, 32, 32);
        g.setStroke(new BasicStroke(3.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.translate(64, 960);
        Path2D cube = new Path2D.Float();
        cube.moveTo(32, 6); cube.lineTo(54, 19); cube.lineTo(54, 45); cube.lineTo(32, 58);
        cube.lineTo(10, 45); cube.lineTo(10, 19); cube.closePath();
        g.draw(cube); g.drawLine(10, 19, 32, 32); g.drawLine(54, 19, 32, 32); g.drawLine(32, 32, 32, 58);
        g.translate(64, 0);
        g.drawRoundRect(7, 12, 50, 36, 7, 7); g.drawLine(25, 56, 39, 56); g.drawLine(32, 48, 32, 56);
        Path2D graph = new Path2D.Float();
        graph.moveTo(15, 34); graph.lineTo(23, 34); graph.lineTo(29, 24); graph.lineTo(36, 38); graph.lineTo(43, 28); graph.lineTo(49, 28); g.draw(graph);
        g.translate(64, 0);
        g.drawRoundRect(15, 5, 34, 54, 30, 30); g.drawLine(32, 6, 32, 23); g.drawLine(15, 26, 49, 26);
        g.translate(64, 0);
        g.drawRoundRect(24, 5, 17, 23, 5, 5);
        g.drawRoundRect(3, 34, 17, 23, 5, 5); g.drawRoundRect(24, 34, 17, 23, 5, 5); g.drawRoundRect(45, 34, 17, 23, 5, 5);
        g.translate(64, 0);
        g.setStroke(new BasicStroke(6, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Path2D check = new Path2D.Float();
        check.moveTo(14, 32); check.lineTo(27, 44); check.lineTo(50, 20);
        g.draw(check);
        g.translate(64, 0);
        g.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawRoundRect(1, 1, 62, 62, 30, 30);
        g.translate(64, 0);
        Path2D gear = new Path2D.Float();
        for (int i = 0; i < 32; i++) {
            double angle = i * Math.PI / 16;
            double radius = i % 4 < 2 ? 26 : 21;
            double x = 32 + Math.cos(angle) * radius, y = 32 + Math.sin(angle) * radius;
            if (i == 0) gear.moveTo(x, y); else gear.lineTo(x, y);
        }
        gear.closePath(); g.draw(gear); g.drawOval(23, 23, 18, 18);
        g.translate(64, 0); // 7: latency
        for (int i = 0; i < 4; i++) g.fillRoundRect(8 + i * 13, 44 - i * 11, 8, 12 + i * 11, 3, 3);
        g.translate(64, 0); // 8: armor
        armorIcon(g, 1);
        g.translate(64, 0); // 9: coordinates
        g.drawOval(14, 14, 36, 36); g.drawOval(27, 27, 10, 10);
        g.drawLine(32, 3, 32, 20); g.drawLine(32, 44, 32, 61);
        g.drawLine(3, 32, 20, 32); g.drawLine(44, 32, 61, 32);
        g.translate(64, 0); // 10: server
        for (int i = 0; i < 3; i++) {
            g.drawRoundRect(7, 5 + i * 19, 50, 15, 4, 4);
            g.fillOval(13, 10 + i * 19, 5, 5); g.drawLine(27, 13 + i * 19, 49, 13 + i * 19);
        }
        for (int i = 0; i < 4; i++) { g.translate(64, 0); armorIcon(g, i); } // 11..14: empty equipment slots
        g.dispose();
        ImageIO.write(image, "png", output.resolve("atlas.png").toFile());
    }
    private static void armorIcon(Graphics2D g, int slot) {
        Path2D shape = new Path2D.Float();
        int[][] points;
        if (slot == 0) points = new int[][] {{10, 49}, {10, 18}, {20, 8}, {44, 8}, {54, 18}, {54, 49}, {42, 49}, {42, 31}, {22, 31}, {22, 49}};
        else if (slot == 1) points = new int[][] {{9, 7}, {23, 7}, {23, 18}, {41, 18}, {41, 7}, {55, 7}, {59, 29}, {47, 34}, {47, 57}, {17, 57}, {17, 34}, {5, 29}};
        else if (slot == 2) points = new int[][] {{13, 7}, {51, 7}, {51, 57}, {36, 57}, {36, 30}, {28, 30}, {28, 57}, {13, 57}};
        else points = new int[][] {{13, 8}, {28, 8}, {28, 56}, {4, 56}, {4, 41}, {13, 41}};
        shape.moveTo(points[0][0], points[0][1]);
        for (int i = 1; i < points.length; i++) shape.lineTo(points[i][0], points[i][1]);
        shape.closePath(); g.draw(shape);
        if (slot == 3) { g.translate(32, 0); g.draw(shape); g.translate(-32, 0); }
    }
}
