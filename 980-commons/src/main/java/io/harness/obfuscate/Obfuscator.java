package io.harness.obfuscate;

import static java.nio.charset.StandardCharsets.UTF_8;

import io.harness.exception.UnexpectedException;
import lombok.experimental.UtilityClass;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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
