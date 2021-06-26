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

import org.enginehub.discord.module.errorHelper.ErrorHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RawSubdirectoryUrlResolver implements ErrorResolver {

    private final Pattern URL_PATTERN;
    private final String baseUrl;
    private final String subUrl;
    private final boolean secure;

    public RawSubdirectoryUrlResolver(String baseUrl, String subUrl) {
        this(baseUrl, subUrl, false);
    }

    public RawSubdirectoryUrlResolver(String baseUrl, String subUrl, boolean secure) {
        this.baseUrl = baseUrl;
        this.subUrl = subUrl;
        this.URL_PATTERN = Pattern.compile(baseUrl + "/([A-Za-z0-9._-]*)");
        this.secure = secure;
    }

    @Override
    public List<String> foundText(String message) {
        List<String> foundText = new ArrayList<>();
        Matcher matcher = URL_PATTERN.matcher(message);
        while (matcher.find()) {
            foundText.add(ErrorHelper.getStringFromUrl((secure ? "https" : "http")  + "://" + baseUrl + '/' + subUrl + '/' + matcher.group(1)));
        }

        return foundText;
    }
}
