package com.support.agent.common.util;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class OrderNumbers {

    private static final Pattern ORDER_NO = Pattern.compile("\\bORD-[A-Za-z0-9]{4,}\\b", Pattern.CASE_INSENSITIVE);

    private OrderNumbers() {
    }

    public static Optional<String> extract(String message) {
        if (message == null) {
            return Optional.empty();
        }
        Matcher matcher = ORDER_NO.matcher(message);
        if (matcher.find()) {
            return Optional.of(matcher.group().toUpperCase());
        }
        return Optional.empty();
    }
}
