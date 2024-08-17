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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class PasteUtil {

    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
        .registerModules(new Jdk8Module(), new ParameterNamesModule());

    private static HttpClient client = HttpClient.newHttpClient();

    public static CompletableFuture<URL> sendToPastebin(String content) throws IOException, URISyntaxException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(new URL("https://paste.enginehub.org/signed_paste_v2").toURI())
            .GET()
            .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(response -> {
            if (response.statusCode() != 200) {
                throw new RuntimeException("Failed start paste signing: " + response.body());
            }

            try {
                return OBJECT_MAPPER.readValue(response.body(), SignedPasteV2Response.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }).thenCompose(signedPasteData -> {
            try {
                URL url = new URL(signedPasteData.uploadUrl);
                HttpRequest.Builder uploadRequestBuilder = HttpRequest.newBuilder()
                    .uri(url.toURI())
                    .PUT(HttpRequest.BodyPublishers.ofString(content));

                for (Map.Entry<String, String> header : signedPasteData.headers.entrySet()) {
                    uploadRequestBuilder = uploadRequestBuilder.header(header.getKey(), header.getValue());
                }

                return client.sendAsync(uploadRequestBuilder.build(), HttpResponse.BodyHandlers.ofString()).thenApply(uploadResponse -> {
                    // If this succeeds, it will not return any data aside from a 204 status.
                    if (uploadResponse.statusCode() != 200 && uploadResponse.statusCode() != 204) {
                        throw new RuntimeException("Failed to upload paste: " + uploadResponse.body());
                    }
                    try {
                        return new URL(signedPasteData.viewUrl);
                    } catch (MalformedURLException e) {
                        throw new RuntimeException(e);
                    }
                });
            } catch (URISyntaxException | IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private record SignedPasteV2Response(String viewUrl, String uploadUrl, Map<String, String> headers) {
    }
}
