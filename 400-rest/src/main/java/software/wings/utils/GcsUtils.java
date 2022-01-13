/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.utils;

import static io.harness.data.encoding.EncodingUtils.decodeBase64;
import static io.harness.data.encoding.EncodingUtils.encodeBase64;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;

import software.wings.app.MainConfiguration;
import software.wings.helpers.ext.url.SubdomainUrlHelperIntfc;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.net.URLEncoder;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class GcsUtils {
  @Inject private MainConfiguration configuration;
  @Inject private SubdomainUrlHelperIntfc subdomainUrlHelper;
  private static final String downloadPathPattern = "/storage/harness-download";

  private String getSignedUrlUsingPrivateKey(
      String objectPath, String account, String privateKey, long durationInSeconds, String accountId) throws Exception {
    String expiryTime = getExpiryTimeInEpoch(durationInSeconds);
    String signInput = getSignInput(expiryTime, objectPath);
    PrivateKey pk = getPrivateKey(privateKey);
    String signedString = getSignedString(signInput, pk);
    // URL encode the signed string so that we can add this URL
    signedString = URLEncoder.encode(signedString, "UTF-8");
    String portalUrl = subdomainUrlHelper.getPortalBaseUrlWithoutSeparator(accountId);
    return constructSignedUrl(portalUrl + downloadPathPattern + objectPath, account, expiryTime, signedString);
  }

  public String getSignedUrlForServiceAccount(
      String objectPath, String serviceAccountJsonFile, long durationInSeconds, String accountId) throws Exception {
    if (isEmpty(serviceAccountJsonFile)) {
      log.warn(
          "ServiceAccount json file not found,cannot generate signedUrl for {}, returning empty string", objectPath);
      throw new WingsException(ErrorCode.INVALID_INFRA_CONFIGURATION);
    }
    return getSignedUrlUsingPrivateKey(objectPath, getAccountNameFromJsonFile(serviceAccountJsonFile),
        getPkFromJsonFile(serviceAccountJsonFile), durationInSeconds, accountId);
  }

  // Set an expiry date for the signed url. Sets it at one minute ahead of
  // current time.
  // Represented as the epoch time (seconds since 1st January 1970)
  private String getExpiryTimeInEpoch(long durationInSeconds) {
    if (durationInSeconds < 1) {
      durationInSeconds = 600;
    }
    String expiryTime;
    long now = System.currentTimeMillis();
    // note the conversion to seconds as needed by GCS.
    long expiredTimeInSeconds = (now + durationInSeconds * 1000L) / 1000;
    return Long.toString(expiredTimeInSeconds);
  }

  // The signed URL format as required by Google.
  private String constructSignedUrl(String fullObjectPath, String account, String expiryTime, String signedString) {
    return fullObjectPath + "?GoogleAccessId=" + account + "&Expires=" + expiryTime + "&Signature=" + signedString;
  }

  // We sign the expiry time and bucket object path
  private String getSignInput(String expiryTime, String objectPath) {
    return "GET"
        + "\n"
        + ""
        + "\n"
        + ""
        + "\n" + expiryTime + "\n" + objectPath;
  }

  // Use SHA256withRSA to sign the request
  private String getSignedString(String input, PrivateKey pk) throws Exception {
    Signature privateSignature = Signature.getInstance("SHA256withRSA");
    privateSignature.initSign(pk);
    privateSignature.update(input.getBytes("UTF-8"));
    return encodeBase64(privateSignature.sign());
  }

  // Get private key object from unencrypted PKCS#8 file content
  private PrivateKey getPrivateKey(String privateKey) throws Exception {
    // Remove extra characters in private key.
    String realPK = privateKey.replaceAll("-----END PRIVATE KEY-----", "")
                        .replaceAll("-----BEGIN PRIVATE KEY-----", "")
                        .replaceAll("\n", "");
    byte[] b1 = decodeBase64(realPK);
    PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(b1);
    KeyFactory kf = KeyFactory.getInstance("RSA");
    return kf.generatePrivate(spec);
  }

  private String getPkFromJsonFile(String jsonFileAsString) {
    return new Gson().fromJson(jsonFileAsString, JsonObject.class).get("private_key").getAsString();
  }

  private String getAccountNameFromJsonFile(String jsonFileAsString) {
    return new Gson().fromJson(jsonFileAsString, JsonObject.class).get("client_email").getAsString();
  }
}
