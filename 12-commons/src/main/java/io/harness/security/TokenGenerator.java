package io.harness.security;

import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.KeyLengthException;
import com.nimbusds.jose.crypto.DirectEncrypter;
import com.nimbusds.jwt.EncryptedJWT;
import com.nimbusds.jwt.JWTClaimsSet;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;
import javax.crypto.spec.SecretKeySpec;

@Slf4j
public class TokenGenerator {
  private String accountId;
  private String accountSecret;

  public TokenGenerator(String accountId, String accountSecret) {
    this.accountId = accountId;
    this.accountSecret = accountSecret;
  }

  public String getToken(String scheme, String host, int port, String issuer) {
    return getToken(scheme + "://" + host + ":" + port, issuer);
  }

  public String getToken(String audience, String issuer) {
    Instant now = Instant.now();
    JWTClaimsSet jwtClaims = new JWTClaimsSet.Builder()
                                 .issuer(issuer)
                                 .subject(accountId)
                                 .audience(audience)
                                 .expirationTime(Date.from(now.plus(5, ChronoUnit.MINUTES)))
                                 .notBeforeTime(Date.from(now))
                                 .issueTime(Date.from(now))
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
