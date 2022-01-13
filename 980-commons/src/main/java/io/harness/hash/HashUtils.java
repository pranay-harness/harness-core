/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.hash;

import io.harness.exception.InvalidRequestException;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import lombok.experimental.UtilityClass;

@UtilityClass
public class HashUtils {
  public static String calculateSha256(String input) {
    {
      try {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] messageDigest = md.digest(input.getBytes());
        BigInteger no = new BigInteger(1, messageDigest);
        String hashtext = no.toString(16);
        while (hashtext.length() < 32) {
          hashtext = "0" + hashtext;
        }
        return hashtext;
      } catch (NoSuchAlgorithmException e) {
        throw new InvalidRequestException("Cannot hash the input", e);
      }
    }
  }
}
