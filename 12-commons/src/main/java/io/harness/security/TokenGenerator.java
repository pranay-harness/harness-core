package io.harness.security;

import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.KeyLengthException;
import com.nimbusds.jose.crypto.DirectEncrypter;
import com.nimbusds.jwt.EncryptedJWT;
import com.nimbusds.jwt.JWTClaimsSet;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.crypto.spec.SecretKeySpec;

public class TokenGenerator {
  private static final Logger logger = LoggerFactory.getLogger(TokenGenerator.class);

  private String accountId;
  private String accountSecret;

  public TokenGenerator(String accountId, String accountSecret) {
    this.accountId = accountId;
    this.accountSecret = accountSecret;
  }

  public String getToken(String scheme, String host, int port, String issuer) {
    JWTClaimsSet jwtClaims = new JWTClaimsSet.Builder()
                                 .issuer(issuer)
                                 .subject(accountId)
                                 .audience(scheme + "://" + host + ":" + port)
                                 .expirationTime(new Date(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5)))
                                 .notBeforeTime(new Date())
                                 .issueTime(new Date())
                                 .jwtID(UUID.randomUUID().toString())
                                 .build();

    JWEHeader header = new JWEHeader(JWEAlgorithm.DIR, EncryptionMethod.A128GCM);
    EncryptedJWT jwt = new EncryptedJWT(header, jwtClaims);
    DirectEncrypter directEncrypter = null;
    byte[] encodedKey = new byte[0];
    try {
      encodedKey = Hex.decodeHex(accountSecret.toCharArray());
    } catch (DecoderException e) {
      logger.error("", e);
    }
    try {
      directEncrypter = new DirectEncrypter(new SecretKeySpec(encodedKey, 0, encodedKey.length, "AES"));
    } catch (KeyLengthException e) {
      logger.error("", e);
    }

    try {
      jwt.encrypt(directEncrypter);
    } catch (JOSEException e) {
      logger.error("", e);
    }

    return jwt.serialize();
  }
}
