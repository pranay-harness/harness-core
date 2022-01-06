/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.authenticator;

import static io.harness.annotations.dev.HarnessTeam.DEL;
import static io.harness.eraro.ErrorCode.DEFAULT_ERROR_CODE;
import static io.harness.eraro.ErrorCode.EXPIRED_TOKEN;
import static io.harness.exception.WingsException.USER_ADMIN;
import static io.harness.manage.GlobalContextManager.initGlobalContextGuard;
import static io.harness.manage.GlobalContextManager.upsertGlobalContextRecord;

import static software.wings.beans.Account.GLOBAL_ACCOUNT_ID;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.context.GlobalContext;
import io.harness.delegate.beans.DelegateNgToken;
import io.harness.delegate.beans.DelegateNgToken.DelegateNgTokenKeys;
import io.harness.delegate.beans.DelegateToken;
import io.harness.delegate.beans.DelegateToken.DelegateTokenKeys;
import io.harness.delegate.beans.DelegateTokenStatus;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.InvalidTokenException;
import io.harness.exception.RevokedTokenException;
import io.harness.exception.WingsException;
import io.harness.ff.FeatureFlagService;
import io.harness.globalcontex.DelegateTokenGlobalContextData;
import io.harness.manage.GlobalContextManager;
import io.harness.persistence.HIterator;
import io.harness.persistence.HPersistence;
import io.harness.persistence.NameAndValueAccess;
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

@Slf4j
@Singleton
@OwnedBy(DEL)
@TargetModule(HarnessModule._420_DELEGATE_SERVICE)
public class DelegateTokenAuthenticatorImpl implements DelegateTokenAuthenticator {
  @Inject private FeatureFlagService featureFlagService;
  @Inject private HPersistence persistence;

  private final LoadingCache<String, String> keyCache =
      Caffeine.newBuilder()
          .maximumSize(10000)
          .expireAfterWrite(5, TimeUnit.MINUTES)
          .build(accountId
              -> Optional.ofNullable(persistence.get(Account.class, accountId))
                     .map(Account::getAccountKey)
                     .orElse(null));

  @Override
  public void validateDelegateToken(String accountId, String tokenString) {
    EncryptedJWT encryptedJWT;
    try {
      encryptedJWT = EncryptedJWT.parse(tokenString);
    } catch (ParseException e) {
      throw new InvalidTokenException("Invalid delegate token format", USER_ADMIN);
    }

    boolean successfullyDecrypted = decryptJWTDelegateToken(accountId, DelegateTokenStatus.ACTIVE, encryptedJWT);
    if (!successfullyDecrypted) {
      boolean decryptedWithRevokedToken = decryptJWTDelegateToken(accountId, DelegateTokenStatus.REVOKED, encryptedJWT);
      if (decryptedWithRevokedToken) {
        String delegateHostName = "";
        try {
          delegateHostName = encryptedJWT.getJWTClaimsSet().getIssuer();
        } catch (ParseException e) {
          log.warn("Couldn't parse token", e);
        }
        log.warn("Delegate {} is using REVOKED delegate token", delegateHostName);
        throw new RevokedTokenException("Invalid delegate token. Delegate is using revoked token", USER_ADMIN);
      }

      decryptWithAccountKey(accountId, encryptedJWT);
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

  private void decryptWithAccountKey(String accountId, EncryptedJWT encryptedJWT) {
    String accountKey = null;
    try {
      accountKey = keyCache.get(accountId);
    } catch (Exception ex) {
      log.warn("Account key not found for accountId: {}", accountId, ex);
    }

    if (accountKey == null || GLOBAL_ACCOUNT_ID.equals(accountId)) {
      throw new InvalidRequestException("Access denied", USER_ADMIN);
    }

    decryptDelegateToken(encryptedJWT, accountKey);
  }

  private boolean decryptJWTDelegateToken(String accountId, DelegateTokenStatus status, EncryptedJWT encryptedJWT) {
    long time_start = System.currentTimeMillis();
    Query<DelegateToken> query = persistence.createQuery(DelegateToken.class)
                                     .field(DelegateTokenKeys.accountId)
                                     .equal(accountId)
                                     .field(DelegateTokenKeys.status)
                                     .equal(status);

    boolean result = decryptDelegateTokenByQuery(query, accountId, status, encryptedJWT);
    if (!result) {
      Query<DelegateNgToken> queryNg = persistence.createQuery(DelegateNgToken.class)
                                           .field(DelegateNgTokenKeys.accountId)
                                           .equal(accountId)
                                           .field(DelegateNgTokenKeys.status)
                                           .equal(status);
      result = decryptDelegateTokenByQuery(queryNg, accountId, status, encryptedJWT);
    }
    long time_end = System.currentTimeMillis() - time_start;
    log.debug("Delegate Token verification for accountId {} and status {} has taken {} milliseconds.", accountId,
        status.name(), time_end);
    return result;
  }

  private boolean decryptDelegateTokenByQuery(
      Query query, String accountId, DelegateTokenStatus status, EncryptedJWT encryptedJWT) {
    try (HIterator<NameAndValueAccess> records = new HIterator<>(query.fetch())) {
      for (NameAndValueAccess delegateToken : records) {
        try {
          decryptDelegateToken(encryptedJWT, delegateToken.getValue());

          if (DelegateTokenStatus.ACTIVE == status) {
            if (!GlobalContextManager.isAvailable()) {
              initGlobalContextGuard(new GlobalContext());
            }
            upsertGlobalContextRecord(
                DelegateTokenGlobalContextData.builder().tokenName(delegateToken.getName()).build());
          }
          return true;
        } catch (Exception e) {
          log.debug("Fail to decrypt Delegate JWT using delete token {} for the account {}", delegateToken.getName(),
              accountId);
        }
      }
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
