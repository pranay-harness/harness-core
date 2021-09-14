/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.utils;

import java.security.SecureRandom;
import java.util.Random;

/**
 * The current implementation is taken from 400-rest
 */
/* http://stackoverflow.com/a/19597101 */
public class CryptoUtils {
  private static final char[] VALID_CHARACTERS =
      "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456879".toCharArray();

  /**
   * Secure rand alpha num string.
   *
   * @param length the length
   * @return the string
   */
  public static String secureRandAlphaNumString(int length) {
    SecureRandom srand = new SecureRandom();
    Random rand = new Random();
    char[] buff = new char[length];

    for (int i = 0; i < length; ++i) {
      // reseed rand once you've used up all available entropy bits
      if ((i % 10) == 0) {
        rand.setSeed(srand.nextLong()); // 64 bits of random!
      }
      buff[i] = VALID_CHARACTERS[rand.nextInt(VALID_CHARACTERS.length)];
    }
    return new String(buff);
  }
}
