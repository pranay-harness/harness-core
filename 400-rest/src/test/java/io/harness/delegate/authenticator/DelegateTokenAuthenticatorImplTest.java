/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.authenticator;

import static io.harness.annotations.dev.HarnessTeam.DEL;
import static io.harness.rule.OwnerRule.ANUBHAW;
import static io.harness.rule.OwnerRule.BRETT;
import static io.harness.rule.OwnerRule.LUCAS;
import static io.harness.rule.OwnerRule.MARKO;
import static io.harness.rule.OwnerRule.UJJAWAL;

import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.FeatureName;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.DelegateToken;
import io.harness.delegate.beans.DelegateTokenStatus;
import io.harness.eraro.ErrorCode;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.InvalidTokenException;
import io.harness.exception.WingsException;
import io.harness.ff.FeatureFlagService;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;
import io.harness.security.TokenGenerator;

import software.wings.WingsBaseTest;
import software.wings.beans.Service;

import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.inject.Inject;
import java.security.NoSuchAlgorithmException;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.MorphiaIterator;
import org.mongodb.morphia.query.Query;

@OwnedBy(DEL)
@TargetModule(HarnessModule._420_DELEGATE_SERVICE)
public class DelegateTokenAuthenticatorImplTest extends WingsBaseTest {
  private static final String GLOBAL_ACCOUNT_ID = "__GLOBAL_ACCOUNT_ID__";

  @Mock LoadingCache<String, String> keyCache;
  @Mock private HPersistence persistence;
  @Mock FeatureFlagService featureFlagService;
  @Inject @InjectMocks private DelegateTokenAuthenticatorImpl delegateTokenAuthenticator;

  private String accountKey = "2f6b0988b6fb3370073c3d0505baee59";

  @Before
  public void setUp() throws Exception {
    initMocks(this);
    FieldUtils.writeField(delegateTokenAuthenticator, "keyCache", keyCache, true);
    when(keyCache.get(ACCOUNT_ID)).thenReturn(accountKey);
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldValidateDelegateToken() {
    TokenGenerator tokenGenerator = new TokenGenerator(ACCOUNT_ID, accountKey);
    delegateTokenAuthenticator.validateDelegateToken(
        ACCOUNT_ID, tokenGenerator.getToken("https", "localhost", 9090, "hostname"));
  }

  @Test
  @Owner(developers = LUCAS)
  @Category(UnitTests.class)
  public void shouldValidateDelegateToken_Active() {
    DelegateToken delegateToken = DelegateToken.builder()
                                      .accountId(ACCOUNT_ID)
                                      .name("default")
                                      .value(accountKey)
                                      .status(DelegateTokenStatus.ACTIVE)
                                      .build();

    when(featureFlagService.isEnabled(Matchers.any(FeatureName.class), anyString())).thenReturn(true);

    Query mockQuery = mock(Query.class);
    FieldEnd<Service> fieldEnd = mock(FieldEnd.class);

    MorphiaIterator<DelegateToken, DelegateToken> morphiaIterator = mock(MorphiaIterator.class);

    doReturn(mockQuery).when(persistence).createQuery(DelegateToken.class);
    doReturn(fieldEnd).when(mockQuery).field(anyString());
    doReturn(mockQuery).when(fieldEnd).equal(any());

    when(morphiaIterator.hasNext()).thenReturn(true).thenReturn(false);
    when(morphiaIterator.next()).thenReturn(delegateToken);
    when(mockQuery.fetch()).thenReturn(morphiaIterator);

    TokenGenerator tokenGenerator = new TokenGenerator(ACCOUNT_ID, accountKey);

    delegateTokenAuthenticator.validateDelegateToken(
        ACCOUNT_ID, tokenGenerator.getToken("https", "localhost", 9090, "hostname"));
  }

  @Test
  @Owner(developers = LUCAS)
  @Category(UnitTests.class)
  public void shouldValidateDelegateTokenThrowsInvalidTokenException() {
    DelegateToken delegateToken = DelegateToken.builder()
                                      .accountId(ACCOUNT_ID)
                                      .name("default")
                                      .value(accountKey)
                                      .status(DelegateTokenStatus.REVOKED)
                                      .build();

    when(featureFlagService.isEnabled(Matchers.any(FeatureName.class), anyString())).thenReturn(true);

    Query mockQuery = mock(Query.class);
    FieldEnd<Service> fieldEnd = mock(FieldEnd.class);

    MorphiaIterator<DelegateToken, DelegateToken> morphiaIterator = mock(MorphiaIterator.class);

    doReturn(mockQuery).when(persistence).createQuery(DelegateToken.class);
    doReturn(fieldEnd).when(mockQuery).field(anyString());
    doReturn(mockQuery).when(fieldEnd).equal(any());

    when(morphiaIterator.hasNext()).thenReturn(false);
    when(morphiaIterator.next()).thenReturn(delegateToken);
    when(mockQuery.fetch()).thenReturn(morphiaIterator);

    TokenGenerator tokenGenerator = new TokenGenerator(ACCOUNT_ID, accountKey);

    assertThatThrownBy(()
                           -> delegateTokenAuthenticator.validateDelegateToken(
                               ACCOUNT_ID, tokenGenerator.getToken("https", "localhost", 9090, "hostname")))
        .isInstanceOf(InvalidTokenException.class);
  }

  @Test
  @Owner(developers = LUCAS)
  @Category(UnitTests.class)
  public void shouldValidateDelegateToken_Revoked() {
    DelegateToken delegateTokenRevoked = DelegateToken.builder()
                                             .accountId(ACCOUNT_ID)
                                             .name("TokenName")
                                             .value(accountKey)
                                             .status(DelegateTokenStatus.REVOKED)
                                             .build();
    when(featureFlagService.isEnabled(Matchers.any(FeatureName.class), anyString())).thenReturn(true);

    Query mockQuery = mock(Query.class);
    FieldEnd<Service> fieldEnd = mock(FieldEnd.class);

    MorphiaIterator<DelegateToken, DelegateToken> morphiaIterator = mock(MorphiaIterator.class);

    doReturn(mockQuery).when(persistence).createQuery(DelegateToken.class);
    doReturn(fieldEnd).when(mockQuery).field(anyString());
    doReturn(mockQuery).when(fieldEnd).equal(any());

    when(morphiaIterator.hasNext()).thenReturn(true).thenReturn(false);
    when(morphiaIterator.next()).thenReturn(delegateTokenRevoked);
    when(mockQuery.fetch()).thenReturn(morphiaIterator);

    TokenGenerator tokenGenerator = new TokenGenerator(ACCOUNT_ID, accountKey);

    delegateTokenAuthenticator.validateDelegateToken(
        ACCOUNT_ID, tokenGenerator.getToken("https", "localhost", 9090, "hostname"));
  }

  @Test
  @Owner(developers = LUCAS)
  @Category(UnitTests.class)
  public void shouldValidateDelegateToken_FailToDecrypt() {
    DelegateToken delegateTokenActive = DelegateToken.builder()
                                            .accountId(ACCOUNT_ID)
                                            .name("TokenName")
                                            .value("InvalidTokenValue")
                                            .status(DelegateTokenStatus.ACTIVE)
                                            .build();
    when(featureFlagService.isEnabled(Matchers.any(FeatureName.class), anyString())).thenReturn(true);

    Query mockQuery = mock(Query.class);
    FieldEnd<Service> fieldEnd = mock(FieldEnd.class);

    MorphiaIterator<DelegateToken, DelegateToken> morphiaIterator = mock(MorphiaIterator.class);

    doReturn(mockQuery).when(persistence).createQuery(DelegateToken.class);
    doReturn(fieldEnd).when(mockQuery).field(anyString());
    doReturn(mockQuery).when(fieldEnd).equal(any());

    when(morphiaIterator.hasNext()).thenReturn(true).thenReturn(false);
    when(morphiaIterator.next()).thenReturn(delegateTokenActive);
    when(mockQuery.fetch()).thenReturn(morphiaIterator);

    TokenGenerator tokenGenerator = new TokenGenerator(ACCOUNT_ID, accountKey);

    assertThatThrownBy(()
                           -> delegateTokenAuthenticator.validateDelegateToken(
                               ACCOUNT_ID, tokenGenerator.getToken("https", "localhost", 9090, "hostname")))
        .isInstanceOf(InvalidTokenException.class);
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(UnitTests.class)
  public void shouldNotValidateDelegateToken() {
    TokenGenerator tokenGenerator = new TokenGenerator(GLOBAL_ACCOUNT_ID, accountKey);
    assertThatThrownBy(()
                           -> delegateTokenAuthenticator.validateDelegateToken(
                               GLOBAL_ACCOUNT_ID, tokenGenerator.getToken("https", "localhost", 9090, "hostname")))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("Access denied");
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void shouldNotValidateExpiredDelegateToken() {
    String expiredToken =
        "eyJlbmMiOiJBMTI4R0NNIiwiYWxnIjoiZGlyIn0..SFvYSml0znPxoa7K.JcsFw5GiYevubqqzjy-nQyDMzjtA64YhxZjnQz6VH7lRCAGP5JML9Ov86rSRV1V7Kb-a12UvTNzqEqdJ4PCLv4R7GA5SzCwxLEYrlTLtUWX40r0GKuRGoiJVJqax2bBy3gOqDftETZCm_90lD3NxDeJ__RICl4osp9IxCKmlfGyoqriAswoEvkVtu0wjRlvBS-FtY42AeyCf9XIH5rppw-AsXoHH40M6_8FN-mFkilfqv3QKPaGL6Zph.1ipAjbMS834AKSotvHy4sg";
    assertThatThrownBy(() -> delegateTokenAuthenticator.validateDelegateToken(ACCOUNT_ID, expiredToken))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("Unauthorized");
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(UnitTests.class)
  public void shouldThrowDenyAccessWhenAccountIdNullForDelegate() {
    TokenGenerator tokenGenerator = new TokenGenerator(ACCOUNT_ID, accountKey);
    assertThatThrownBy(()
                           -> delegateTokenAuthenticator.validateDelegateToken(
                               null, tokenGenerator.getToken("https", "localhost", 9090, "hostname")))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("Access denied");
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldThrowDenyAccessWhenAccountIdNotFoundForDelegate() {
    TokenGenerator tokenGenerator = new TokenGenerator(ACCOUNT_ID, accountKey);
    assertThatThrownBy(()
                           -> delegateTokenAuthenticator.validateDelegateToken(
                               ACCOUNT_ID + "1", tokenGenerator.getToken("https", "localhost", 9090, "hostname")))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("Access denied");
  }

  @Test
  @Owner(developers = ANUBHAW)
  @Category(UnitTests.class)
  public void shouldThrowThrowInavlidTokenForDelegate() {
    assertThatThrownBy(() -> delegateTokenAuthenticator.validateDelegateToken(ACCOUNT_ID, "Dummy"))
        .isInstanceOf(InvalidTokenException.class);
  }

  @Test
  @Owner(developers = ANUBHAW)
  @Category(UnitTests.class)
  public void shouldThrowExceptionWhenUnableToDecryptToken() {
    assertThatThrownBy(() -> delegateTokenAuthenticator.validateDelegateToken(ACCOUNT_ID, getDelegateToken()))
        .isInstanceOf(InvalidTokenException.class);
  }

  private String getDelegateToken() {
    KeyGenerator keyGen = null;
    try {
      keyGen = KeyGenerator.getInstance("AES");
    } catch (NoSuchAlgorithmException e) {
      throw new WingsException(ErrorCode.DEFAULT_ERROR_CODE);
    }
    keyGen.init(128);
    SecretKey secretKey = keyGen.generateKey();
    byte[] encoded = secretKey.getEncoded();
    TokenGenerator tokenGenerator = new TokenGenerator(ACCOUNT_ID, Hex.encodeHexString(encoded));
    return tokenGenerator.getToken("https", "localhost", 9090, "hostname");
  }
}
