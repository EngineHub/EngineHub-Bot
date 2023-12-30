package org.enginehub.discord.util;

import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;

public class PasteUtilTest {

    // This test is ignored as it actually creates a paste on paste.enginehub.org
    @Ignore
    @Test
    public void testCreatesPaste() {
        try {
            System.out.println(PasteUtil.sendToPastebin("test").get());
        } catch (IOException | URISyntaxException | InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
}
