/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
