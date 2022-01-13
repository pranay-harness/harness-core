/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
