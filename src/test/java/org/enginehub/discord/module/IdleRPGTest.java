package org.enginehub.discord.module;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Test;

import java.time.Instant;

import static org.junit.Assert.assertEquals;

public class IdleRPGTest {

    private static void assertPlayerDataEquals(IdleRPG.PlayerData expected, IdleRPG.PlayerData actual) {
        assertEquals(expected.getLastName(), actual.getLastName());
        assertEquals(expected.getLevel(), actual.getLevel());
        assertEquals(expected.getLevelTime(), actual.getLevelTime());
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
