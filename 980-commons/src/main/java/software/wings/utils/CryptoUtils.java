/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.utils;

import java.security.SecureRandom;
import java.util.Random;

/**
 * The Class CryptoUtils.
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
