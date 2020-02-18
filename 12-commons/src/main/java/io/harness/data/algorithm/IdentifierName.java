package io.harness.data.algorithm;

import lombok.experimental.UtilityClass;
import org.apache.commons.codec.binary.Base32;

import java.security.SecureRandom;

@UtilityClass
public class IdentifierName {
  private static Base32 base32 = new Base32();
  private static SecureRandom random = new SecureRandom();
  private static String prefix = "VAR";

  public static String random() {
    byte[] bytes = new byte[10];
    random.nextBytes(bytes);
    return prefix + base32.encodeAsString(bytes);
  }
}
