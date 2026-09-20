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
import okhttp3.Request;
import okhttp3.RequestBody;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class PasteUtil {

    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
        .registerModules(new Jdk8Module(), new ParameterNamesModule());

    public static CompletableFuture<URI> sendToPastebin(String content) {
        Request request = new Request.Builder()
            .url("https://paste.enginehub.org/signed_paste_v2")
            .build();

        return HttpUtil.sendAsync(request).thenApply(response -> {
            if (response.code() != 200) {
                throw new RuntimeException("Failed start paste signing: " + response.body());
            }

            try {
                return OBJECT_MAPPER.readValue(response.body(), SignedPasteV2Response.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }).thenCompose(signedPasteData -> {
            Request.Builder uploadRequestBuilder = new Request.Builder()
                .url(signedPasteData.uploadUrl)
                .put(RequestBody.create(content, null));

            for (Map.Entry<String, String> header : signedPasteData.headers.entrySet()) {
                uploadRequestBuilder = uploadRequestBuilder.header(header.getKey(), header.getValue());
            }

            return HttpUtil.sendAsync(uploadRequestBuilder.build()).thenApply(uploadResponse -> {
                // If this succeeds, it will not return any data aside from a 204 status.
                if (uploadResponse.code() != 200 && uploadResponse.code() != 204) {
                    throw new RuntimeException("Failed to upload paste: " + uploadResponse.body());
                }
                try {
                    return new URI(signedPasteData.viewUrl);
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
            });
        });
    }

    private record SignedPasteV2Response(String viewUrl, String uploadUrl, Map<String, String> headers) {
    }

    private PasteUtil() {
    }
}
