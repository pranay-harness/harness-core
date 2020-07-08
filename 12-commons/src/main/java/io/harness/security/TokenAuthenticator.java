package io.harness.security;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEDecrypter;
import com.nimbusds.jose.KeyLengthException;
import com.nimbusds.jose.crypto.DirectDecrypter;
import com.nimbusds.jwt.EncryptedJWT;
import io.harness.exception.AccessDeniedException;
import io.harness.exception.UnauthorizedException;
import io.harness.exception.UnexpectedException;
import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import java.text.ParseException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

@Slf4j
@Singleton
public class TokenAuthenticator {
  private final KeySource keyFetcher;

  @Inject
  public TokenAuthenticator(KeySource keyFetcher) {
    this.keyFetcher = keyFetcher;
  }

  public String validateToken(String accountId, String tokenString) {
    EncryptedJWT encryptedJWT;
    encryptedJWT = parseToken(tokenString);
    SecretKey accountKey = makeKeySpec(fetchKey(accountId));

    JWEDecrypter decrypter;
    try {
      decrypter = new DirectDecrypter(accountKey);
    } catch (KeyLengthException e) {
      logger.error("Invalid key " + accountKey, e);
      throw new UnexpectedException();
    }

    try {
      encryptedJWT.decrypt(decrypter);
    } catch (JOSEException e) {
      throw new UnauthorizedException(null, WingsException.USER_SRE);
    }
    return accountId;
  }

  private EncryptedJWT parseToken(String tokenString) {
    EncryptedJWT encryptedJWT;
    try {
      encryptedJWT = EncryptedJWT.parse(tokenString);
    } catch (ParseException e) {
      logger.error("Invalid JWE token " + tokenString, e);
      throw new UnauthorizedException(null, WingsException.USER_SRE);
    }
    return encryptedJWT;
  }

  private String fetchKey(String accountId) {
    logger.debug("Fetching key for accountId: {}", accountId);
    String accountKey = keyFetcher.fetchKey(accountId);
    if (accountKey == null) {
      logger.error("Key not found for accountId: {}", accountId);
      throw new AccessDeniedException(null, WingsException.USER_SRE);
    }

    return accountKey;
  }

  private SecretKey makeKeySpec(String accountKey) {
    byte[] encodedKey;
    try {
      encodedKey = Hex.decodeHex(accountKey.toCharArray());
    } catch (DecoderException e) {
      logger.error("Invalid hex key " + accountKey, e);
      throw new UnexpectedException();
    }
    return new SecretKeySpec(encodedKey, 0, encodedKey.length, "AES");
  }
}
