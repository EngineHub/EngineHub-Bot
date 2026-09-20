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

package org.enginehub.discord.util;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.UncheckedExecutionException;
import net.dv8tion.jda.api.entities.Message;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.Response;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import javax.imageio.ImageIO;

/// Fetches images attached to Discord messages and caches them in memory. This is used to avoid repeatedly downloading
/// the same image when various modules process the same message.
///
/// We might want to consider changing message events to instead carry an object that can hold the reference, so we're
/// not reliant on GC to clean up the cache.
public final class AttachmentImages {

    // We probably don't need more than this many pixels, and it bounds how large an image can be.
    private static final int MAX_SIDE = 4096;

    private static final Cache<Message.Attachment, BufferedImage> IMAGES = CacheBuilder.newBuilder()
        .weakKeys()
        .build();

    public static BufferedImage fetch(Message.Attachment attachment) throws IOException {
        try {
            return IMAGES.get(attachment, () -> load(attachment));
        } catch (ExecutionException | UncheckedExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IOException ioException) {
                throw ioException;
            }
            throw new RuntimeException("Unexpected error while loading image: " + attachment.getFileName(), cause);
        }
    }

    private static BufferedImage load(Message.Attachment attachment) throws IOException {
        Request request = new Request.Builder().url(getPngUrl(attachment)).build();
        try (Response response = HttpUtil.getClient().newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException(
                    "HTTP " + response.code() + " while downloading image: " + attachment.getFileName()
                );
            }
            BufferedImage image = ImageIO.read(response.body().byteStream());
            if (image == null) {
                throw new IOException("Unreadable image: " + attachment.getFileName());
            }
            return image;
        }
    }

    private static HttpUrl getPngUrl(Message.Attachment attachment) {
        HttpUrl.Builder url = HttpUrl.get(attachment.getProxyUrl()).newBuilder()
            .setQueryParameter("format", "png");
        int largestSide = Math.max(attachment.getWidth(), attachment.getHeight());
        if (largestSide > MAX_SIDE) {
            double scale = (double) MAX_SIDE / largestSide;
            url.setQueryParameter("width", String.valueOf(scaleSide(attachment.getWidth(), scale)));
            url.setQueryParameter("height", String.valueOf(scaleSide(attachment.getHeight(), scale)));
        }
        return url.build();
    }

    private static int scaleSide(int side, double scale) {
        return Math.max(1, (int) Math.round(side * scale));
    }

    private AttachmentImages() {
    }
}
