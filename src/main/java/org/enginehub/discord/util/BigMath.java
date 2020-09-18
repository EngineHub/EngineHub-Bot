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
