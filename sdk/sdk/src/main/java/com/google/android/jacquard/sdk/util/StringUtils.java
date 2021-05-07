/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.google.android.jacquard.sdk.util;

import com.google.android.jacquard.sdk.log.PrintLogger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Util class with methods related to string.
 */
public final class StringUtils {

    private static final String TAG =  StringUtils.class.getSimpleName();

    private static StringUtils stringUtils;

    private StringUtils() {
    }

    /**
     * Returns instance of StringUtils.
     */
    public static StringUtils getInstance() {
        if (stringUtils == null) {
            stringUtils = new StringUtils();
        }
        return stringUtils;
    }

    /**
     * Converts Integer to '-' separated Hex string.
     */
    public String integerToHexString(int integer) {
        String hex = Integer.toHexString(integer);
        StringBuilder builder = new StringBuilder();
        int index = 0;
        for (char c : hex.toCharArray()) {
            if (index > 0 && index % 2 == 0) {
                builder.append("-");
            }
            builder.append(c);
            index++;
        }
        return builder.toString();
    }

    /**
     * Converts '-' separated Hex String to Integer.
     */
    public int hexStringToInteger(String integer) {
        String replaceId = integer.replace(/*target = */"-", /*replacement = */"");
        return (int) Long.parseLong(replaceId, /*radix = */16);
    }

    /** Calculates SHA of the text. */
    public String sha1(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] textBytes = text.getBytes();
            md.update(textBytes, 0, textBytes.length);
            byte[] sha1hash = md.digest();
            return convertToHex(sha1hash);
        } catch (NoSuchAlgorithmException e) {
            // As mentioned in MessageDigest document, this exception will never be thrown
            // for reference: https://docs.oracle.com/javase/8/docs/api/java/security/MessageDigest.html
            PrintLogger.e(TAG, e.getMessage());
        }
        return "";
    }

    private String convertToHex(byte[] data) {
        StringBuilder buf = new StringBuilder();
        for (byte b : data) {
            int halfbyte = (b >>> 4) & 0x0F;
            int twoHalfs = 0;
            do {
                buf.append(
                    (0 <= halfbyte) && (halfbyte <= 9)
                        ? (char) ('0' + halfbyte)
                        : (char) ('a' + (halfbyte - 10)));
                halfbyte = b & 0x0F;
            } while (twoHalfs++ < 1);
        }
        return buf.toString();
    }
}
