package io.harness.data.algorithm;

import org.apache.commons.codec.binary.Base32;

import java.util.Random;

public class IdentifierName {
  private static Base32 base32 = new Base32();
  private static Random random = new Random();
  private static String prefix = "VAR";

  public static String random() {
    byte[] bytes = new byte[10];
    random.nextBytes(bytes);
    return prefix + base32.encodeAsString(bytes);
  }
}
