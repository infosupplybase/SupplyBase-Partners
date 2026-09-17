package com.supplybase.partners.common.domain;

import java.util.regex.Pattern;

/** Normalizes Indian mobile numbers to E.164 (+91XXXXXXXXXX). */
public final class PhoneNumbers {

    private static final Pattern TEN_DIGIT = Pattern.compile("^[6-9]\\d{9}$");

    private PhoneNumbers() {
    }

    public static String normalizeIndian(String raw) {
        if (raw == null) {
            throw new BadRequestException("Phone number is required.");
        }
        String digits = raw.replaceAll("[\\s\\-()]", "");
        if (digits.startsWith("+91")) {
            digits = digits.substring(3);
        } else if (digits.startsWith("91") && digits.length() == 12) {
            digits = digits.substring(2);
        } else if (digits.startsWith("0") && digits.length() == 11) {
            digits = digits.substring(1);
        }
        if (!TEN_DIGIT.matcher(digits).matches()) {
            throw new BadRequestException("Enter a valid 10-digit Indian mobile number.");
        }
        return "+91" + digits;
    }
}
