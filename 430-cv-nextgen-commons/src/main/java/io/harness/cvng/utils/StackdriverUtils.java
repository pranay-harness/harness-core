package io.harness.cvng.utils;

import io.harness.cvng.beans.stackdriver.StackdriverCredential;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;

@Slf4j
public class StackdriverUtils {
  public enum Scope {
    METRIC_SCOPE("https://www.googleapis.com/auth/monitoring.read"),
    LOG_SCOPE("https://www.googleapis.com/auth/logging.read");

    private final String value;

    Scope(final String v) {
      this.value = v;
    }
    public String getValue() {
      return this.value;
    }
  }

  public static final String TIMESTAMP_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";

  private StackdriverUtils() {}

  public static Map<String, Object> getCommonEnvVariables(StackdriverCredential credential, Scope scope) {
    Map<String, Object> envVariables = new HashMap<>();
    String jwtToken = StackdriverUtils.getJwtToken(credential, scope);
    envVariables.put("jwtToken", jwtToken);
    envVariables.put("project", credential.getProjectId());
    return envVariables;
  }

  private static String getJwtToken(StackdriverCredential credential, Scope scope) {
    Algorithm algorithm;
    try {
      algorithm = Algorithm.RSA256(getPrivateKeyFromString(credential.getPrivateKey()));
    } catch (Exception ex) {
      log.error("Exception while reading private key for stackdriver", ex);
      throw new RuntimeException("Exception while reading private key for stackdriver");
    }
    Map<String, Object> headers = new HashMap<>();
    headers.put("alg", "RS256");
    headers.put("typ", "JWT");
    headers.put("kid", credential.getPrivateKeyId());

    return JWT.create()
        .withIssuer(credential.getClientEmail())
        .withSubject(credential.getClientEmail())
        .withClaim("scope", scope.getValue())
        .withIssuedAt(new Date())
        .withExpiresAt(new Date(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5)))
        .withAudience("https://www.googleapis.com/oauth2/v4/token")
        .withHeader(headers)
        .sign(algorithm);
  }

  private static RSAPrivateKey getPrivateKeyFromString(String key) throws IOException, GeneralSecurityException {
    String privateKeyPEM = key;
    privateKeyPEM = privateKeyPEM.replace("-----BEGIN PRIVATE KEY-----\n", "");
    privateKeyPEM = privateKeyPEM.replace("-----END PRIVATE KEY-----", "");
    privateKeyPEM = privateKeyPEM.replaceAll("\n", "");
    byte[] encoded = Base64.decodeBase64(privateKeyPEM);
    KeyFactory kf = KeyFactory.getInstance("RSA");
    PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
    return (RSAPrivateKey) kf.generatePrivate(keySpec);
  }

  public static <T> T checkForNullAndReturnValue(T value, T defaultValue) {
    return value == null ? defaultValue : value;
  }
}
