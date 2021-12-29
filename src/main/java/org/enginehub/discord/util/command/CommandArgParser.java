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
package org.enginehub.discord.util.command;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CommandArgParser {

    public static CommandArgParser forArgString(String argString) {
        return new CommandArgParser(spaceSplit(argString));
    }

    public static ImmutableList<Substring> spaceSplit(String string) {
        ImmutableList.Builder<Substring> result = ImmutableList.builder();
        int index = 0;
        for (String part : Splitter.on(' ').split(string)) {
            result.add(Substring.from(string, index, index + part.length()));
            index += part.length() + 1;
        }
        return result.build();
    }

    private enum State {
        NORMAL,
        QUOTE
    }

    private final Stream.Builder<Substring> args = Stream.builder();
    private final List<Substring> input;
    private final List<Substring> currentArg = new ArrayList<>();
    private int index = 0;
    private State state = State.NORMAL;

    public CommandArgParser(List<Substring> input) {
        this.input = input;
    }

    public Stream<Substring> parseArgs() {
        for (; index < input.size(); index++) {
            Substring nextPart = input.get(index);
            switch (state) {
                case NORMAL -> handleNormal(nextPart);
                case QUOTE -> handleQuote(nextPart);
                default -> {
                }
            }
        }
        if (currentArg.size() > 0) {
            finishArg(); // force finish "hanging" args
        }
        return args.build();
    }

    private void handleNormal(Substring part) {
        final String strPart = part.getSubstring();
        if (strPart.startsWith("\"")) {
            if (strPart.endsWith("\"") && strPart.length() > 1) {
                currentArg.add(Substring.wrap(
                    strPart.substring(1, strPart.length() - 1),
                    part.getStart() + 1, part.getEnd() - 1
                ));
                finishArg();
            } else {
                state = State.QUOTE;
                currentArg.add(Substring.wrap(
                    strPart.substring(1),
                    part.getStart() + 1, part.getEnd()
                ));
            }
        } else {
            currentArg.add(part);
            finishArg();
        }
    }

    private void handleQuote(Substring part) {
        if (part.getSubstring().endsWith("\"")) {
            state = State.NORMAL;
            currentArg.add(Substring.wrap(
                part.getSubstring().substring(0, part.getSubstring().length() - 1),
                part.getStart(), part.getEnd() - 1
            ));
            finishArg();
        } else {
            currentArg.add(part);
        }
    }

    private void finishArg() {
        // Merge the arguments into a single, space-joined, string
        // Keep the original start + end points.
        int start = currentArg.get(0).getStart();
        int end = Iterables.getLast(currentArg).getEnd();
        args.add(Substring.wrap(currentArg.stream()
                .map(Substring::getSubstring)
                .collect(Collectors.joining(" ")),
            start, end
        ));
        currentArg.clear();
    }

}
