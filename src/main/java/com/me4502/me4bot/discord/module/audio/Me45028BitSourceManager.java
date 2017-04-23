/*
 * Copyright (c) 2016-2017 Me4502 (Matthew Miller)
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
package com.me4502.me4bot.discord.module.audio;

import com.sedmelluq.discord.lavaplayer.container.MediaContainerDetectionResult;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.http.HttpAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.track.AudioItem;
import com.sedmelluq.discord.lavaplayer.track.AudioReference;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class Me45028BitSourceManager extends HttpAudioSourceManager {

    private static final String SEARCH_PREFIX = "8bit:";

    private static Method detectContainerMethod;
    private static Field authorField;

    Me45028BitSourceManager() {
        try {
            detectContainerMethod = HttpAudioSourceManager.class.getDeclaredMethod("detectContainer", AudioReference.class);
            detectContainerMethod.setAccessible(true);

            authorField = AudioTrackInfo.class.getField("author");
            authorField.setAccessible(true);

            Field timeField = AudioTrackInfo.class.getField("length");
            timeField.setAccessible(true);

            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(authorField, authorField.getModifiers() & ~Modifier.FINAL);
            modifiersField.setInt(timeField, timeField.getModifiers() & ~Modifier.FINAL);
        } catch (NoSuchMethodException | NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getSourceName() {
        return "Me4502 8 Bit";
    }

    @Override
    public AudioItem loadItem(DefaultAudioPlayerManager manager, AudioReference reference) {
        if (reference.identifier.startsWith(SEARCH_PREFIX)) {
            String searchTerm = reference.identifier.substring(SEARCH_PREFIX.length()).trim();

            try (CloseableHttpResponse response = getHttpInterface().execute(new HttpGet("http://me4502.com/midi-lib?search=" + searchTerm.replace(" ", "%20")))) {
                if (response.getStatusLine().getStatusCode() != 200) {
                    return null;
                } else {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
                    String line;
                    StringBuilder output = new StringBuilder();
                    while ((line = reader.readLine()) != null) {
                        output.append(line);
                    }

                    if (output.toString().equals("NONE")) {
                        return null;
                    }

                    System.out.println(output);

                    String[] outputBits = output.toString().split(";");
                    String url = outputBits[0];
                    String name = outputBits[1];
                    String artist = outputBits[2];

                    System.out.println(url + " name " + name);

                    MediaContainerDetectionResult result = (MediaContainerDetectionResult) detectContainerMethod.invoke(this, new AudioReference(url, name));

                    authorField.set(result.getTrackInfo(), artist);

                    return handleLoadResult(result);
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }

        return null;
    }
}
