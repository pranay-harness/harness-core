/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.data.algorithm;

import java.security.SecureRandom;
import lombok.experimental.UtilityClass;
import org.apache.commons.codec.binary.Base32;

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
