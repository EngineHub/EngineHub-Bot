package com.me4502.me4bot.discord.module.audio;

import com.sedmelluq.discord.lavaplayer.container.MediaContainerDetectionResult;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.http.HttpAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.track.AudioItem;
import com.sedmelluq.discord.lavaplayer.track.AudioReference;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class Me45028BitSourceManager extends HttpAudioSourceManager {

    private static final String SEARCH_PREFIX = "8bit:";

    private static Method detectContainerMethod;
    private static Field authorField;

    public Me45028BitSourceManager() {
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

            try (CloseableHttpClient client = createHttpClient()) {
                CloseableHttpResponse response = client.execute(new HttpGet("http://me4502.com/midi-lib?search=" + searchTerm.replace(" ", "%20")));

                if (response.getStatusLine().getStatusCode() != 200) {
                    return null;
                } else {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
                    String line;
                    String output = "";
                    while ((line = reader.readLine()) != null) {
                        output += line;
                    }

                    if (output.equals("NONE")) {
                        return null;
                    }

                    System.out.println(output);

                    String[] outputBits = output.split(";");
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
