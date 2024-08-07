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
package org.enginehub.discord.module.errorHelper.resolver;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IncompatibleResolver implements ErrorResolver {

    private static final String INCOMPATIBLE_TEXT = "This paste provider does not allow automated reading, preventing the bot from assisting you. We recommend using our paste site instead, https://paste.enginehub.org/";

    private final Pattern URL_PATTERN;

    public IncompatibleResolver(String baseUrl) {
        this.URL_PATTERN = Pattern.compile(baseUrl + "/([A-Za-z0-9._-]*)");
    }

    @Override
    public List<String> foundText(String message) {
        List<String> foundText = new ArrayList<>();
        Matcher matcher = URL_PATTERN.matcher(message);
        while (matcher.find()) {
            foundText.add(INCOMPATIBLE_TEXT);
        }

        return foundText;
    }
}
