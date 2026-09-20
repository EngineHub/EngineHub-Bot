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
package org.enginehub.discord.module.errorHelper;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ErrorHelperTest {

    private HttpServer server;

    @BeforeEach
    public void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.start();
    }

    @AfterEach
    public void stopServer() {
        server.stop(0);
    }

    private String url(String path) {
        return "http://" + server.getAddress().getHostString() + ':' + server.getAddress().getPort() + path;
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
        if (bytes.length > 0) {
            exchange.getResponseBody().write(bytes);
        }
        exchange.close();
    }

    @Test
    public void testDropsLineTerminators() {
        server.createContext("/log", exchange -> respond(exchange, 200, "first\nsecond\r\nthïrd\n"));

        assertEquals("firstsecondthïrd", ErrorHelper.getStringFromUrl(url("/log")));
    }

    @Test
    public void testFollowsRedirect() {
        server.createContext("/old", exchange -> {
            exchange.getResponseHeaders().add("Location", "/new");
            respond(exchange, 302, "");
        });
        server.createContext("/new", exchange -> respond(exchange, 200, "moved"));

        assertEquals("moved", ErrorHelper.getStringFromUrl(url("/old")));
    }

    @Test
    public void testRetriesServerError() {
        AtomicInteger requests = new AtomicInteger();
        server.createContext("/flaky", exchange -> {
            if (requests.getAndIncrement() == 0) {
                respond(exchange, 500, "broken");
            } else {
                respond(exchange, 200, "fine");
            }
        });

        assertEquals("fine", ErrorHelper.getStringFromUrl(url("/flaky")));
        assertEquals(2, requests.get());
    }

    @Test
    public void testGivesUpAfterRetries() {
        AtomicInteger requests = new AtomicInteger();
        server.createContext("/dead", exchange -> {
            requests.getAndIncrement();
            respond(exchange, 404, "missing");
        });

        assertEquals("", ErrorHelper.getStringFromUrl(url("/dead")));
        assertEquals(6, requests.get());
    }

    @Test
    public void testKeepsPartialContentWhenStreamFails() {
        AtomicInteger requests = new AtomicInteger();
        server.createContext("/truncated", exchange -> {
            requests.getAndIncrement();
            exchange.sendResponseHeaders(200, 1000);
            exchange.getResponseBody().write("partial\n".getBytes(StandardCharsets.UTF_8));
            exchange.getResponseBody().flush();
            exchange.close();
        });

        assertEquals("partial", ErrorHelper.getStringFromUrl(url("/truncated")));
        assertEquals(6, requests.get());
    }
}
