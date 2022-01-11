/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.utils;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Date;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class SignedUrls {
  private SignedUrls() {
    // Prevent instantiation
  }

  public static String signUrl(String url, byte[] key, String keyName, Date expirationTime)
      throws InvalidKeyException, NoSuchAlgorithmException {
    long unixTime = expirationTime.getTime() / 1000;

    String urlToSign = url + (url.contains("?") ? "&" : "?") + "Expires=" + unixTime + "&KeyName=" + keyName;
    String encoded = getSignature(key, urlToSign);
    return urlToSign + "&Signature=" + encoded;
  }

  private static String getSignature(byte[] privateKey, String input)
      throws InvalidKeyException, NoSuchAlgorithmException {
    String algorithm = "HmacSHA1";
    int offset = 0;
    Key key = new SecretKeySpec(privateKey, offset, privateKey.length, algorithm);
    Mac mac = Mac.getInstance(algorithm);
    mac.init(key);
    return Base64.getUrlEncoder().encodeToString(mac.doFinal(input.getBytes(UTF_8)));
  }
}
