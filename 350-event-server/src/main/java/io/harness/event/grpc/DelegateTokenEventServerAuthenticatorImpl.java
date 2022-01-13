/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.event.grpc;

import static io.harness.annotations.dev.HarnessTeam.DEL;
import static io.harness.eraro.ErrorCode.DEFAULT_ERROR_CODE;
import static io.harness.eraro.ErrorCode.EXPIRED_TOKEN;
import static io.harness.exception.WingsException.USER_ADMIN;
import static io.harness.manage.GlobalContextManager.initGlobalContextGuard;
import static io.harness.manage.GlobalContextManager.upsertGlobalContextRecord;

import static software.wings.beans.Account.GLOBAL_ACCOUNT_ID;

import io.harness.annotations.dev.OwnedBy;
import io.harness.context.GlobalContext;
import io.harness.delegate.beans.DelegateToken;
import io.harness.delegate.beans.DelegateToken.DelegateTokenKeys;
import io.harness.delegate.beans.DelegateTokenStatus;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.InvalidTokenException;
import io.harness.exception.RevokedTokenException;
import io.harness.exception.WingsException;
import io.harness.globalcontex.DelegateTokenGlobalContextData;
import io.harness.manage.GlobalContextManager;
import io.harness.persistence.HIterator;
import io.harness.persistence.HPersistence;
import io.harness.security.DelegateTokenAuthenticator;

import software.wings.beans.Account;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEDecrypter;
import com.nimbusds.jose.KeyLengthException;
import com.nimbusds.jose.crypto.DirectDecrypter;
import com.nimbusds.jwt.EncryptedJWT;
import java.text.ParseException;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.mongodb.morphia.query.Query;

/**
 * This class is temporary solution and should be deleted and
 * {@link io.harness.delegate.authenticator.DelegateTokenAuthenticatorImpl} should be used instead,
 * when dependency to FeatureFlagService is removed from {@link
 * io.harness.delegate.authenticator.DelegateTokenAuthenticatorImpl}. It was added as temporary solution, in order to
 * avoid adding dependency to the FeatureFlagService in this module (350-event-server).
 */
@Slf4j
@Singleton
@OwnedBy(DEL)
public class DelegateTokenEventServerAuthenticatorImpl implements DelegateTokenAuthenticator {
  @Inject private HPersistence persistence;

  private final LoadingCache<String, String> keyCache =
      Caffeine.newBuilder()
          .maximumSize(10000)
          .expireAfterWrite(5, TimeUnit.MINUTES)
          .build(accountId
              -> Optional.ofNullable(persistence.get(Account.class, accountId))
                     .map(Account::getAccountKey)
                     .orElse(null));

  private final LoadingCache<String, DelegateTokenStatus> defaultTokenStatusCache =
      Caffeine.newBuilder()
          .maximumSize(10000)
          .expireAfterWrite(2, TimeUnit.MINUTES)
          .build(accountId
              -> Optional
                     .ofNullable(persistence.createQuery(DelegateToken.class)
                                     .filter(DelegateTokenKeys.accountId, accountId)
                                     .filter(DelegateTokenKeys.name, "default")
                                     .get())
                     .map(DelegateToken::getStatus)
                     .orElse(null));

  @Override
  public void validateDelegateToken(String accountId, String tokenString) {
    String accountKey = null;
    DelegateTokenStatus defaultTokenStatus = null;
    try {
      accountKey = keyCache.get(accountId);
      defaultTokenStatus = defaultTokenStatusCache.get(accountId);
    } catch (Exception ex) {
      // noop
    }

    if (accountKey == null || GLOBAL_ACCOUNT_ID.equals(accountId)) {
      throw new InvalidRequestException("Access denied", USER_ADMIN);
    }

    EncryptedJWT encryptedJWT;
    try {
      encryptedJWT = EncryptedJWT.parse(tokenString);
    } catch (ParseException e) {
      throw new InvalidTokenException("Invalid delegate token format", USER_ADMIN);
    }

    // This implementation was used instead of feature flag service. AccountKey which is the old logic, will be used
    // first, since it is cached and everything will be done fast and in-memory. If that fails, logic will try
    // decrypting using delegate tokens, which is new logic for verifying delegate tokens.
    try {
      // Default token value is same as accountKey. Default token can be revoked only from UI, if FF to use custom
      // tokens is enabled. So, if the status of default token is revoked, that means that we should use custom token
      // check logic, so we throw exception here, to jump to the catch part.
      if (defaultTokenStatus == DelegateTokenStatus.REVOKED) {
        throw new RuntimeException("Default delegate token revoked. Checking other custom tokens...");
      }
      decryptDelegateToken(encryptedJWT, accountKey);
    } catch (Exception ex) {
      boolean decryptedWithActiveToken = decryptJWTDelegateToken(accountId, DelegateTokenStatus.ACTIVE, encryptedJWT);

      if (!decryptedWithActiveToken) {
        boolean decryptedWithRevokedToken =
            decryptJWTDelegateToken(accountId, DelegateTokenStatus.REVOKED, encryptedJWT);
        if (decryptedWithRevokedToken) {
          String delegateHostName = "";
          try {
            delegateHostName = encryptedJWT.getJWTClaimsSet().getIssuer();
          } catch (ParseException e) {
            // NOOP
          }
          log.warn("Delegate {} is using REVOKED delegate token", delegateHostName);
        }

        if (decryptedWithRevokedToken) {
          throw new RevokedTokenException("Invalid delegate token. Delegate is using revoked token", USER_ADMIN);
        }
        throw new InvalidTokenException("Invalid delegate token.", USER_ADMIN);
      }
    }

    try {
      Date expirationDate = encryptedJWT.getJWTClaimsSet().getExpirationTime();
      if (System.currentTimeMillis() > expirationDate.getTime()) {
        throw new InvalidRequestException("Unauthorized", EXPIRED_TOKEN, null);
      }
    } catch (ParseException ex) {
      throw new InvalidRequestException("Unauthorized", ex, EXPIRED_TOKEN, null);
    }
  }

  private boolean decryptJWTDelegateToken(String accountId, DelegateTokenStatus status, EncryptedJWT encryptedJWT) {
    long time_start = System.currentTimeMillis();

    Query<DelegateToken> query = persistence.createQuery(DelegateToken.class)
                                     .field(DelegateTokenKeys.accountId)
                                     .equal(accountId)
                                     .field(DelegateTokenKeys.status)
                                     .equal(status);

    try (HIterator<DelegateToken> records = new HIterator<>(query.fetch())) {
      for (DelegateToken delegateToken : records) {
        try {
          decryptDelegateToken(encryptedJWT, delegateToken.getValue());

          if (DelegateTokenStatus.ACTIVE == status) {
            if (!GlobalContextManager.isAvailable()) {
              initGlobalContextGuard(new GlobalContext());
            }
            upsertGlobalContextRecord(
                DelegateTokenGlobalContextData.builder().tokenName(delegateToken.getName()).build());
          }

          long time_end = System.currentTimeMillis() - time_start;
          log.info("Delegate Token verification for accountId {} and status {} has taken {} milliseconds.", accountId,
              status.name(), time_end);
          return true;
        } catch (Exception e) {
          log.debug("Fail to decrypt Delegate JWT using delete token {} for the account {}", delegateToken.getName(),
              accountId);
        }
      }
      long time_end = System.currentTimeMillis() - time_start;
      log.info("Delegate Token verification for accountId {} and status {} has taken {} milliseconds.", accountId,
          status.name(), time_end);

      return false;
    }
  }

  private void decryptDelegateToken(EncryptedJWT encryptedJWT, String delegateToken) {
    byte[] encodedKey;
    try {
      encodedKey = Hex.decodeHex(delegateToken.toCharArray());
    } catch (DecoderException e) {
      throw new WingsException(DEFAULT_ERROR_CODE, USER_ADMIN, e);
    }

    JWEDecrypter decrypter;
    try {
      decrypter = new DirectDecrypter(new SecretKeySpec(encodedKey, 0, encodedKey.length, "AES"));
    } catch (KeyLengthException e) {
      throw new WingsException(DEFAULT_ERROR_CODE, USER_ADMIN, e);
    }

    try {
      encryptedJWT.decrypt(decrypter);
    } catch (JOSEException e) {
      throw new InvalidTokenException("Invalid delegate token", USER_ADMIN);
    }
  }
}
