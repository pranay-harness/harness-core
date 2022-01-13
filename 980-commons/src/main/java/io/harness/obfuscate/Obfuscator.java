/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.obfuscate;

import static java.nio.charset.StandardCharsets.UTF_8;

import io.harness.exception.UnexpectedException;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import lombok.experimental.UtilityClass;

@UtilityClass
public class Obfuscator {
  public static String obfuscate(String input) {
    if (input == null) {
      return null;
    }

    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      byte[] hashInBytes = md.digest(input.getBytes(UTF_8));

      StringBuilder sb = new StringBuilder();
      for (byte b : hashInBytes) {
        sb.append(String.format("%02x", b));
      }
      return sb.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new UnexpectedException("MD5 should be always available", e);
    }
  }
}
