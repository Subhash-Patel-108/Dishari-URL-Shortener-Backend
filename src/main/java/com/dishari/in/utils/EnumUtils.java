package com.dishari.in.utils;

import com.dishari.in.exception.InvalidEnumValueException;

import java.util.Optional;

public final class EnumUtils {

    private EnumUtils() {}

    // Returns empty Optional instead of throwing on invalid value
    public static <T extends Enum<T>> Optional<T> fromString(
            Class<T> enumClass, String value) {

        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(
                    Enum.valueOf(enumClass, value.trim().toUpperCase()));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    // Throws with a clean message — use when invalid = bad request
    public static <T extends Enum<T>> T fromStringOrThrow(
            Class<T> enumClass, String value, String fieldName) {

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " cannot be blank.");
        }
        try {
            return Enum.valueOf(enumClass, value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new InvalidEnumValueException(
                    "Invalid value '" + value + "' for " + fieldName + ". " +
                            "Allowed values: " + allowedValues(enumClass)
            );
        }
    }

    private static <T extends Enum<T>> String allowedValues(
            Class<T> enumClass) {
        StringBuilder sb = new StringBuilder();
        for (T constant : enumClass.getEnumConstants()) {
            sb.append(constant.name()).append(", ");
        }
        return sb.substring(0, sb.length() - 2);
    }
}