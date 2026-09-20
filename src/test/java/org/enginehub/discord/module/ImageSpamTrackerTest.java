/*
 * Copyright (c) EngineHub and Contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.enginehub.discord.module;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.InstantSource;
import java.util.Random;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ImageSpamTrackerTest {

    private static final long USER = 1;
    private static final long OTHER_USER = 2;

    private static final class FakeClock implements InstantSource {
        private Instant now = Instant.EPOCH;

        @Override
        public Instant instant() {
            return now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }
    }

    private final FakeClock clock = new FakeClock();
    private final ImageSpamTracker tracker = new ImageSpamTracker(clock);

    private static BufferedImage createPhotoLikeImage(long seed, int size) {
        var random = new Random(seed);
        var image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        // draw on a random background color to ensure some difference between images
        graphics.setColor(new Color(random.nextInt(0x1000000)));
        graphics.fillRect(0, 0, size, size);
        // emulate some of the features of a photo using blobs and gradients
        for (int i = 0; i < 12; i++) {
            float radius = size * (0.15f + random.nextFloat() * 0.35f);
            float x = random.nextFloat() * size;
            float y = random.nextFloat() * size;
            // random color again for the blobs
            var color = new Color(random.nextInt(0x1000000));
            var clear = new Color(color.getRed(), color.getGreen(), color.getBlue(), 0);
            // paint a fading blob that doesn't have a color gradient
            graphics.setPaint(new RadialGradientPaint(x, y, radius, new float[] { 0, 1 }, new Color[] { color, clear }));
            graphics.fillRect(0, 0, size, size);
        }
        graphics.dispose();
        return image;
    }

    private static BufferedImage createConsoleLikeImage(long seed) {
        var random = new Random(seed);
        var image = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        // dark background to emulate a console window
        graphics.setColor(new Color(0x1E1F22));
        graphics.fillRect(0, 0, 256, 256);
        // draw some rows that somewhat emulate text
        graphics.setColor(new Color(0xDBDEE1));
        for (int y = 8; y < 248; y += 10) {
            if (random.nextInt(4) != 0) {
                graphics.fillRect(8, y, 20 + random.nextInt(220), 3);
            }
        }
        graphics.dispose();
        return image;
    }

    private static BufferedImage createSolidImage(Color color) {
        var image = new BufferedImage(128, 128, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(color);
        graphics.fillRect(0, 0, 128, 128);
        graphics.dispose();
        return image;
    }

    private static BufferedImage createNoisyImage(BufferedImage source, long seed) {
        var random = new Random(seed);
        var image = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                int rgb = source.getRGB(x, y);
                int result = 0;
                for (int shift = 0; shift <= 16; shift += 8) {
                    int channel = ((rgb >> shift) & 0xFF) + random.nextInt(7) - 3;
                    result |= Math.clamp(channel, 0, 255) << shift;
                }
                image.setRGB(x, y, result);
            }
        }
        return image;
    }

    private static BufferedImage scale(BufferedImage source, int size) {
        var image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.drawImage(source, 0, 0, size, size, null);
        graphics.dispose();
        return image;
    }

    private static BufferedImage applyJpegCompression(BufferedImage source, float quality) throws IOException {
        var bytes = new ByteArrayOutputStream();
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        try (var output = new MemoryCacheImageOutputStream(bytes)) {
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(quality);
            writer.setOutput(output);
            writer.write(null, new IIOImage(source, null, null), param);
        } finally {
            writer.dispose();
        }
        return ImageIO.read(new ByteArrayInputStream(bytes.toByteArray()));
    }

    private static BufferedImage cropped(BufferedImage source) {
        int border = source.getWidth() / 20;
        return source.getSubimage(border, border, source.getWidth() - border * 2, source.getHeight() - border * 2);
    }

    private static BufferedImage withTransparentCenter(BufferedImage source) {
        int size = source.getWidth();
        int radius = size / 3;
        var image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int dx = x - size / 2;
                int dy = y - size / 2;
                int alpha = dx * dx + dy * dy < radius * radius ? 0 : 0xFF;
                image.setRGB(x, y, (alpha << 24) | (source.getRGB(x, y) & 0xFFFFFF));
            }
        }
        return image;
    }

    private static BufferedImage compositeOnWhite(BufferedImage source) {
        var image = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.drawImage(source, 0, 0, Color.WHITE, null);
        graphics.dispose();
        return image;
    }

    @Test
    public void testNoisyCopies() {
        BufferedImage image = createPhotoLikeImage(1, 64);

        assertFalse(tracker.isSpammedImage(USER, 1, image));
        assertFalse(tracker.isSpammedImage(USER, 2, createNoisyImage(image, 10)));
        assertTrue(tracker.isSpammedImage(USER, 3, createNoisyImage(image, 11)));
    }

    @Test
    public void testReencodedCopies() throws IOException {
        BufferedImage image = createPhotoLikeImage(1, 256);

        assertFalse(tracker.isSpammedImage(USER, 1, scale(image, 64)));
        assertFalse(tracker.isSpammedImage(USER, 2, scale(applyJpegCompression(image, 0.4f), 64)));
        assertTrue(tracker.isSpammedImage(USER, 3, scale(applyJpegCompression(scale(image, 154), 0.4f), 64)));
    }

    @Test
    public void testDifferentResolutions() {
        BufferedImage image = createPhotoLikeImage(1, 256);

        assertFalse(tracker.isSpammedImage(USER, 1, scale(image, 64)));
        assertFalse(tracker.isSpammedImage(USER, 2, scale(image, 64)));
        assertTrue(tracker.isSpammedImage(USER, 3, image));
    }

    @Test
    public void testTransparentCopies() {
        BufferedImage image = withTransparentCenter(createPhotoLikeImage(1, 64));

        assertFalse(tracker.isSpammedImage(USER, 1, image));
        assertFalse(tracker.isSpammedImage(USER, 2, compositeOnWhite(image)));
        assertTrue(tracker.isSpammedImage(USER, 3, image));
    }

    @Test
    public void testDifferentImages() {
        assertFalse(tracker.isSpammedImage(USER, 1, createPhotoLikeImage(1, 64)));
        assertFalse(tracker.isSpammedImage(USER, 2, createPhotoLikeImage(2, 64)));
        assertFalse(tracker.isSpammedImage(USER, 3, createPhotoLikeImage(3, 64)));
    }

    @Test
    public void testDifferentConsoleScreenshots() {
        assertFalse(tracker.isSpammedImage(USER, 1, createConsoleLikeImage(1)));
        assertFalse(tracker.isSpammedImage(USER, 2, createConsoleLikeImage(2)));
        assertFalse(tracker.isSpammedImage(USER, 3, createConsoleLikeImage(3)));
    }

    @Test
    public void testOnlyTracksWithinOneMinute() {
        BufferedImage image = createPhotoLikeImage(1, 64);

        assertFalse(tracker.isSpammedImage(USER, 1, image));
        clock.advance(Duration.ofSeconds(30));
        assertFalse(tracker.isSpammedImage(USER, 2, image));
        clock.advance(Duration.ofSeconds(40));
        assertFalse(tracker.isSpammedImage(USER, 3, image));
    }

    @Test
    public void testMessagesAreCountedNotImages() {
        BufferedImage image = createPhotoLikeImage(1, 64);

        assertFalse(tracker.isSpammedImage(USER, 1, image));
        assertFalse(tracker.isSpammedImage(USER, 1, image));
        assertFalse(tracker.isSpammedImage(USER, 1, image));
        assertFalse(tracker.isSpammedImage(USER, 2, image));
        assertFalse(tracker.isSpammedImage(USER, 2, image));
        assertTrue(tracker.isSpammedImage(USER, 3, image));
    }

    @Test
    public void testImagesAreTrackedPerUser() {
        BufferedImage image = createPhotoLikeImage(1, 64);

        assertFalse(tracker.isSpammedImage(USER, 1, image));
        assertFalse(tracker.isSpammedImage(OTHER_USER, 2, image));
        assertFalse(tracker.isSpammedImage(USER, 3, image));
        assertFalse(tracker.isSpammedImage(OTHER_USER, 4, image));
    }

    /// This is a known limitation of our approach, we do not try to detect cropping.
    @Test
    public void testCroppedCopyIsNotSimilar() {
        BufferedImage image = createPhotoLikeImage(1, 256);

        assertFalse(tracker.isSpammedImage(USER, 1, image));
        assertFalse(tracker.isSpammedImage(USER, 2, image));
        assertFalse(tracker.isSpammedImage(USER, 3, cropped(image)));
    }

    /// This is a known limitation of our approach, we cannot detect solid color images as spam, due to the nature of
    /// the similarity detection algorithm.
    @Test
    public void testSolidColor() {
        assertFalse(tracker.isSpammedImage(USER, 1, createSolidImage(Color.WHITE)));
        assertFalse(tracker.isSpammedImage(USER, 2, createSolidImage(Color.WHITE)));
        assertFalse(tracker.isSpammedImage(USER, 3, createSolidImage(Color.WHITE)));

        BufferedImage image = createPhotoLikeImage(1, 64);
        assertFalse(tracker.isSpammedImage(USER, 4, image));
        assertFalse(tracker.isSpammedImage(USER, 5, image));
        assertTrue(tracker.isSpammedImage(USER, 6, image));
    }
}
