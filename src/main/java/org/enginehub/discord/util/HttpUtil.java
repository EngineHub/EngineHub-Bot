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

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public final class HttpUtil {

    private static final String USER_AGENT = "EngineHub-Bot (https://github.com/EngineHub/EngineHub-Bot)";
    private static final OkHttpClient CLIENT = createClient();

    /**
     * Get an HTTP client, using JDA's settings for connection pooling and request dispatching.
     * We share this client across all requests to avoid creating too many connections and threads.
     *
     * <p>
     * Prefer using {@link #send(Request)} or {@link #sendAsync(Request)} for sending requests if possible in order to
     * keep things simple.
     * </p>
     *
     * @return the client
     */
    public static OkHttpClient getClient() {
        return CLIENT;
    }

    private static OkHttpClient createClient() {
        Dispatcher dispatcher = new Dispatcher();
        dispatcher.setMaxRequestsPerHost(25);
        return new OkHttpClient.Builder()
            .addInterceptor(chain -> {
                Request request = chain.request();
                if (request.header("User-Agent") == null) {
                    request = request.newBuilder().header("User-Agent", USER_AGENT).build();
                }
                return chain.proceed(request);
            })
            .dispatcher(dispatcher)
            .connectionPool(new ConnectionPool(5, 10, TimeUnit.SECONDS))
            .followSslRedirects(false)
            .build();
    }

    public static HttpResult send(Request request) throws IOException {
        try (Response response = CLIENT.newCall(request).execute()) {
            return new HttpResult(response.code(), response.body().string());
        }
    }

    public static CompletableFuture<HttpResult> sendAsync(Request request) {
        var future = new CompletableFuture<HttpResult>();
        CLIENT.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(Call call, Response response) {
                try (response) {
                    future.complete(new HttpResult(response.code(), response.body().string()));
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            }

            @Override
            public void onFailure(Call call, IOException e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    private HttpUtil() {
    }
}
