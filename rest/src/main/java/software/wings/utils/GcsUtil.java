package software.wings.utils;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.app.MainConfiguration;
import software.wings.beans.ErrorCode;
import software.wings.exception.WingsException;

import java.net.URLEncoder;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

@Singleton
public class GcsUtil {
  @Inject private MainConfiguration configuration;
  private static final Logger log = LoggerFactory.getLogger(GcsUtil.class);
  private static final String downloadPathPattern = "/storage/harness-download";

  private String getSignedUrlUsingPrivateKey(
      String objectPath, String account, String privateKey, long durationInSeconds) throws Exception {
    String expiryTime = getExpiryTimeInEpoch(durationInSeconds);
    String signInput = getSignInput(expiryTime, objectPath);
    PrivateKey pk = getPrivateKey(privateKey);
    String signedString = getSignedString(signInput, pk);
    // URL encode the signed string so that we can add this URL
    signedString = URLEncoder.encode(signedString, "UTF-8");
    return constructSignedUrl(
        configuration.getPortal().getUrl() + downloadPathPattern + objectPath, account, expiryTime, signedString);
  }

  public String getSignedUrlForServiceAccount(String objectPath, String serviceAccountJsonFile, long durationInSeconds)
      throws Exception {
    if (isEmpty(serviceAccountJsonFile)) {
      log.warn(
          "ServiceAccount json file not found,cannot generate signedUrl for {}, returning empty string", objectPath);
      throw new WingsException(ErrorCode.INVALID_INFRA_CONFIGURATION);
    }
    return getSignedUrlUsingPrivateKey(objectPath, getAccountNameFromJsonFile(serviceAccountJsonFile),
        getPkFromJsonFile(serviceAccountJsonFile), durationInSeconds);
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
    byte[] s = privateSignature.sign();
    return Base64.getEncoder().encodeToString(s);
  }

  // Get private key object from unencrypted PKCS#8 file content
  private PrivateKey getPrivateKey(String privateKey) throws Exception {
    // Remove extra characters in private key.
    String realPK = privateKey.replaceAll("-----END PRIVATE KEY-----", "")
                        .replaceAll("-----BEGIN PRIVATE KEY-----", "")
                        .replaceAll("\n", "");
    byte[] b1 = Base64.getDecoder().decode(realPK);
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
