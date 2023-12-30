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
import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import com.google.common.net.UrlEscapers;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

public class PasteUtil {

    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
        .registerModules(new Jdk8Module(), new ParameterNamesModule());

    private static HttpClient client = HttpClient.newHttpClient();

    public static CompletableFuture<URL> sendToPastebin(String content) throws IOException, URISyntaxException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(new URL("https://paste.enginehub.org/signed_paste").toURI())
            .GET()
            .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(response -> {
            if (response.statusCode() != 200) {
                throw new RuntimeException("Failed start paste signing: " + response.body());
            }

            try {
                return OBJECT_MAPPER.readValue(response.body(), SignedPasteResponse.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }).thenCompose(signedPasteData -> {
            Form form = Form.create();
            for (Map.Entry<String, String> entry : signedPasteData.uploadFields.entrySet()) {
                form.add(entry.getKey(), entry.getValue());
            }
            form.add("file", content);

            try {
                URL url = new URL(signedPasteData.uploadUrl);
                HttpRequest uploadRequest = HttpRequest.newBuilder()
                    .uri(url.toURI())
                    .header("Content-Type", "multipart/form-data; boundary=" + form.getFormDataSeparator())
                    .POST(HttpRequest.BodyPublishers.ofString(form.toFormDataString()))
                    .build();

                return client.sendAsync(uploadRequest, HttpResponse.BodyHandlers.ofString()).thenApply(uploadResponse -> {
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

    private record SignedPasteResponse(String viewUrl, String uploadUrl, Map<String, String> uploadFields) {
    }

    public static final class Form {

        private static final Joiner.MapJoiner URL_ENCODER = Joiner.on('&')
            .withKeyValueSeparator('=');
        private static final Joiner CRLF_JOINER = Joiner.on("\r\n");

        public final Map<String, String> elements = new LinkedHashMap<>();

        private final String formDataSeparator = "EngineHubFormData"
            + ThreadLocalRandom.current().nextInt(10000, 99999);

        private Form() {
        }

        /**
         * Add a key/value to the form.
         *
         * @param key   the key
         * @param value the value
         * @return this object
         */
        public Form add(String key, String value) {
            elements.put(key, value);
            return this;
        }

        public String getFormDataSeparator() {
            return formDataSeparator;
        }

        public String toFormDataString() {
            String separatorWithDashes = "--" + formDataSeparator;
            StringBuilder builder = new StringBuilder();

            for (Map.Entry<String, String> element : elements.entrySet()) {
                CRLF_JOINER.appendTo(
                    builder,
                    separatorWithDashes,
                    "Content-Disposition: form-data; name=\"" + element.getKey() + "\"",
                    "",
                    element.getValue(),
                    ""
                );
            }

            builder.append(separatorWithDashes).append("--");

            return builder.toString();
        }

        public String toUrlEncodedString() {
            return URL_ENCODER.join(
                elements.entrySet().stream()
                    .map(e -> Maps.immutableEntry(
                        UrlEscapers.urlFormParameterEscaper().escape(e.getKey()),
                        UrlEscapers.urlFormParameterEscaper().escape(e.getValue())
                    ))
                    .iterator()
            );
        }

        /**
         * Create a new form.
         *
         * @return a new form
         */
        public static Form create() {
            return new Form();
        }
    }
}
