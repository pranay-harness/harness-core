/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.utils;

import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import io.harness.version.ServiceApiVersion;

import java.security.NoSuchAlgorithmException;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;

public class Misc {
  private Misc() {
    // to avoid initialization
  }
  public static String generateSecretKey() {
    KeyGenerator keyGen = null;
    try {
      keyGen = KeyGenerator.getInstance("AES");
    } catch (NoSuchAlgorithmException e) {
      throw new WingsException(ErrorCode.DEFAULT_ERROR_CODE, e);
    }
    keyGen.init(128);
    SecretKey secretKey = keyGen.generateKey();
    byte[] encoded = secretKey.getEncoded();
    return Hex.encodeHexString(encoded);
  }

  public static ServiceApiVersion parseApisVersion(String acceptHeader) {
    if (StringUtils.isEmpty(acceptHeader)) {
      return null;
    }

    String[] headers = acceptHeader.split(",");
    String header = headers[0].trim();
    if (!header.startsWith("application/")) {
      throw new IllegalArgumentException("Invalid header " + acceptHeader);
    }

    String versionHeader = header.replace("application/", "").trim();
    if (StringUtils.isEmpty(versionHeader)) {
      throw new IllegalArgumentException("Invalid header " + acceptHeader);
    }

    String[] versionSplit = versionHeader.split("\\+");

    String version = versionSplit[0].trim();
    if (version.toUpperCase().charAt(0) == 'V') {
      return ServiceApiVersion.valueOf(version.toUpperCase());
    }

    return ServiceApiVersion.values()[ServiceApiVersion.values().length - 1];
  }
}
