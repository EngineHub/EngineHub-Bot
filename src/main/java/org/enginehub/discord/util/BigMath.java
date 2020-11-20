/*
 * Copyright (c) Me4502 (Matthew Miller)
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

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;

public class BigMath {

    private static final int EXP_TAYLOR_SERIES_LENGTH = 100;
    private static final List<BigDecimal> FACTORIALS;

    static {
        var factorials = new ArrayList<>(List.of(BigDecimal.ONE, BigDecimal.ONE));
        for (int i = 2; i < EXP_TAYLOR_SERIES_LENGTH; i++) {
            factorials.add(factorials.get(i - 1).multiply(BigDecimal.valueOf(i)));
        }
        FACTORIALS = List.copyOf(factorials);
    }

    public static BigDecimal exp(BigDecimal exponent) {
        var result = BigDecimal.ONE;
        for (int i = 1; i < EXP_TAYLOR_SERIES_LENGTH; i++) {
            result = result.add(expTaylorSeries(i, exponent));
        }
        return result;
    }

    private static BigDecimal expTaylorSeries(int term, BigDecimal x) {
        return x.pow(term).divide(FACTORIALS.get(term), MathContext.DECIMAL128);
    }

    private BigMath() {
    }

}
