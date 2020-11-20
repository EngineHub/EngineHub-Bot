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

import org.junit.ComparisonFailure;
import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;

public class BigMathTest {

    private static void assertNearlyEquals(BigDecimal expected, BigDecimal actual) {
        // assume `expected` has the minimum expected precision, so if actual starts with it,
        // that's close enough!
        if (actual.toString().startsWith(expected.toString())) {
            return;
        }
        throw new ComparisonFailure("", expected.toString(), actual.toString());
    }

    @Test
    public void testExp() {
        assertEquals(BigDecimal.ONE, BigMath.exp(BigDecimal.ZERO));
        assertNearlyEquals(
            new BigDecimal("2.718281828459045235360287471352662"),
            BigMath.exp(BigDecimal.ONE)
        );
        assertNearlyEquals(
            new BigDecimal("7.389056098930650227230427460575007"),
            BigMath.exp(BigDecimal.valueOf(2))
        );
        assertNearlyEquals(
            new BigDecimal("22026.465794806716516957900645284244"),
            BigMath.exp(BigDecimal.TEN)
        );

        assertNearlyEquals(
            new BigDecimal("2.6939270528874989619453743968018871"),
            BigMath.exp(new BigDecimal("0.991"))
        );
        assertNearlyEquals(
            new BigDecimal("1.1554616949637527480657727111033436341"),
            BigMath.exp(new BigDecimal("0.1445"))
        );
    }
}
