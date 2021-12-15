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
import io.harness.delegate.beans.DelegateTokenDetails;
import io.harness.delegate.beans.DelegateTokenStatus;
import io.harness.delegate.utils.DelegateEntityOwnerHelper;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.InvalidTokenException;
import io.harness.exception.RevokedTokenException;
import io.harness.exception.WingsException;
import io.harness.globalcontex.DelegateTokenGlobalContextData;
import io.harness.manage.GlobalContextManager;
import io.harness.persistence.HPersistence;
import io.harness.security.DelegateTokenAuthenticator;
import io.harness.security.dto.DelegateTokenInfo;
import io.harness.security.dto.DelegateTokenInfo.DelegateTokenInfoBuilder;
import io.harness.service.intfc.DelegateNgTokenService;
import io.harness.service.intfc.DelegateTokenService;

import software.wings.beans.Account;

import com.github.benmanes.caffeine.cache.CacheLoader;
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
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

@Slf4j
@Singleton
@OwnedBy(DEL)
@TargetModule(HarnessModule._420_DELEGATE_SERVICE)
public class DelegateTokenAuthenticatorImpl implements DelegateTokenAuthenticator {
  @Inject private HPersistence persistence;
  @Inject private DelegateNgTokenService delegateNgTokenService;
  @Inject private DelegateTokenService delegateTokenService;

  private final LoadingCache<String, String> keyCache =
      Caffeine.newBuilder()
          .maximumSize(10000)
          .expireAfterWrite(5, TimeUnit.MINUTES)
          .build(accountId
              -> Optional.ofNullable(persistence.get(Account.class, accountId))
                     .map(Account::getAccountKey)
                     .orElse(null));

  private final LoadingCache<TokenKey, List<DelegateTokenDetails>> delegateTokenCache =
      Caffeine.newBuilder()
          .maximumSize(10000)
          .expireAfterWrite(5, TimeUnit.MINUTES)
          .build(new CacheLoader<TokenKey, List<DelegateTokenDetails>>() {
            @Nullable
            @Override
            public List<DelegateTokenDetails> load(@NonNull TokenKey tokenKey) throws Exception {
              return delegateTokenService.getDelegateTokens(tokenKey.accountId, tokenKey.status, null);
            }
          });

  private final LoadingCache<TokenKey, List<DelegateTokenDetails>> delegateNgTokenCache =
      Caffeine.newBuilder()
          .maximumSize(10000)
          .expireAfterWrite(5, TimeUnit.MINUTES)
          .build(new CacheLoader<TokenKey, List<DelegateTokenDetails>>() {
            @Nullable
            @Override
            public List<DelegateTokenDetails> load(@NonNull TokenKey tokenKey) throws Exception {
              return delegateNgTokenService.getDelegateTokensForAccountByStatus(tokenKey.accountId, tokenKey.status);
            }
          });

  @Override
  public DelegateTokenInfo validateDelegateToken(String accountId, String tokenString) {
    EncryptedJWT encryptedJWT;
    DelegateTokenInfoBuilder tokenInfoBuilder = DelegateTokenInfo.builder();
    try {
      encryptedJWT = EncryptedJWT.parse(tokenString);
    } catch (ParseException e) {
      throw new InvalidTokenException("Invalid delegate token format", USER_ADMIN);
    }

    boolean successfullyDecrypted =
        decryptJWTDelegateToken(accountId, DelegateTokenStatus.ACTIVE, encryptedJWT, tokenInfoBuilder);
    if (!successfullyDecrypted) {
      boolean decryptedWithRevokedToken =
          decryptJWTDelegateToken(accountId, DelegateTokenStatus.REVOKED, encryptedJWT, tokenInfoBuilder);
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
    return tokenInfoBuilder.build();
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

  private boolean decryptJWTDelegateToken(String accountId, DelegateTokenStatus status, EncryptedJWT encryptedJWT,
      DelegateTokenInfoBuilder tokenInfoBuilder) {
    long time_start = System.currentTimeMillis();
    List<DelegateTokenDetails> tokensForAccount = delegateTokenCache.get(new TokenKey(accountId, status));
    boolean result = decryptUsingNgDelegateTokens(tokensForAccount, encryptedJWT, tokenInfoBuilder);
    if (!result) {
      List<DelegateTokenDetails> ngTokensForAccount = delegateNgTokenCache.get(new TokenKey(accountId, status));
      result = decryptUsingNgDelegateTokens(ngTokensForAccount, encryptedJWT, tokenInfoBuilder);
    }
    long time_end = System.currentTimeMillis() - time_start;
    log.debug("Delegate Token verification for accountId {} and status {} has taken {} milliseconds.", accountId,
        status.name(), time_end);
    return result;
  }

  private boolean decryptUsingDelegateTokens(
      List<DelegateTokenDetails> tokens, EncryptedJWT encryptedJWT, DelegateTokenInfoBuilder tokenInfoBuilder) {
    for (DelegateTokenDetails delegateToken : tokens) {
      try {
        decryptDelegateToken(encryptedJWT, delegateToken.getValue());

        if (DelegateTokenStatus.ACTIVE == delegateToken.getStatus()) {
          if (!GlobalContextManager.isAvailable()) {
            initGlobalContextGuard(new GlobalContext());
          }
          upsertGlobalContextRecord(
              DelegateTokenGlobalContextData.builder().tokenName(delegateToken.getName()).build());
        }
        return true;
      } catch (Exception e) {
        log.debug("Fail to decrypt Delegate JWT using delete token {} for the account {}", delegateToken.getName(),
            delegateToken.getAccountId());
      }
    }
    return false;
  }

  private boolean decryptUsingNgDelegateTokens(
      List<DelegateTokenDetails> tokens, EncryptedJWT encryptedJWT, DelegateTokenInfoBuilder tokenInfoBuilder) {
    for (DelegateTokenDetails delegateToken : tokens) {
      try {
        decryptDelegateToken(encryptedJWT, delegateToken.getValue());

        if (DelegateTokenStatus.ACTIVE == delegateToken.getStatus()) {
          if (!GlobalContextManager.isAvailable()) {
            initGlobalContextGuard(new GlobalContext());
          }
          upsertGlobalContextRecord(
              DelegateTokenGlobalContextData.builder()
                  .tokenName(delegateToken.getName())
                  .orgIdentifier(
                      DelegateEntityOwnerHelper.extractOrgIdFromOwnerIdentifier(delegateToken.getOwnerIdentifier()))
                  .projectIdentifier(
                      DelegateEntityOwnerHelper.extractProjectIdFromOwnerIdentifier(delegateToken.getOwnerIdentifier()))
                  .build());
        }
        tokenInfoBuilder.name(delegateToken.getName()).ownerIdentifier(delegateToken.getOwnerIdentifier());
        return true;
      } catch (Exception e) {
        log.debug("Fail to decrypt Delegate JWT using delete token {} for the account/owner {}/{}",
            delegateToken.getName(), delegateToken.getAccountId(),
            delegateToken.getOwnerIdentifier() != null ? delegateToken.getOwnerIdentifier() : "");
      }
    }
    return false;
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

  private final class TokenKey {
    String accountId;
    DelegateTokenStatus status;

    TokenKey(String accountId, DelegateTokenStatus status) {
      this.accountId = accountId;
      this.status = status;
    }
  }
}
