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
