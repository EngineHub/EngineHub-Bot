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

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Test;

import java.time.Instant;

import static org.junit.Assert.assertEquals;

public class IdleRPGTest {

    private static void assertPlayerDataEquals(IdleRPG.PlayerData expected, IdleRPG.PlayerData actual) {
        assertEquals(expected.lastName(), actual.lastName());
        assertEquals(expected.level(), actual.level());
        assertEquals(expected.levelTime(), actual.levelTime());
    }

    @Test
    public void testPlayerDataRoundTrip() throws JsonProcessingException {
        var playerData = new IdleRPG.PlayerData(Instant.parse("2420-03-16T04:20:44Z"), 42, "Tester");

        var roundTripped = IdleRPG.OBJECT_MAPPER.readValue(
            IdleRPG.OBJECT_MAPPER.writeValueAsString(playerData), IdleRPG.PlayerData.class
        );
        assertPlayerDataEquals(playerData, roundTripped);
    }

    @Test
    public void testSavePlayerData() throws JsonProcessingException {
        var playerData = new IdleRPG.PlayerData(Instant.parse("2420-03-16T04:20:44Z"), 42, "Tester");

        var json = IdleRPG.OBJECT_MAPPER.writeValueAsString(playerData);
        assertEquals("{\"levelTime\":14207113244000,\"level\":42,\"lastName\":\"Tester\"}", json);
    }

    @Test
    public void testLoadPlayerData() throws JsonProcessingException {
        var playerData = new IdleRPG.PlayerData(Instant.parse("2420-03-16T04:20:44Z"), 42, "Tester");
        var json = "{\"levelTime\":14207113244000,\"level\":42,\"lastName\":\"Tester\"}";
        var loaded = IdleRPG.OBJECT_MAPPER.readValue(json, IdleRPG.PlayerData.class);

        assertPlayerDataEquals(playerData, loaded);
    }

    // legacy data serialized a straight long
    @Test
    public void testLoadLegacyPlayerData() throws JsonProcessingException {
        var playerData = new IdleRPG.PlayerData(Instant.parse("2020-11-21T02:30:02.154Z"), 42, "Tester");
        var json = "{\"levelTime\":1605925802154,\"level\":42,\"lastName\":\"Tester\"}";
        var loaded = IdleRPG.OBJECT_MAPPER.readValue(json, IdleRPG.PlayerData.class);

        assertPlayerDataEquals(playerData, loaded);
    }
}
