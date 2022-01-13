/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.event.grpc;

import static io.harness.annotations.dev.HarnessTeam.DEL;
import static io.harness.rule.OwnerRule.ANUBHAW;
import static io.harness.rule.OwnerRule.BRETT;
import static io.harness.rule.OwnerRule.LUCAS;
import static io.harness.rule.OwnerRule.MARKO;
import static io.harness.rule.OwnerRule.UJJAWAL;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.DelegateToken;
import io.harness.delegate.beans.DelegateTokenStatus;
import io.harness.eraro.ErrorCode;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.InvalidTokenException;
import io.harness.exception.RevokedTokenException;
import io.harness.exception.WingsException;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;
import io.harness.security.TokenGenerator;

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
import org.mockito.Mock;
import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.MorphiaIterator;
import org.mongodb.morphia.query.Query;

@OwnedBy(DEL)
public class DelegateTokenEventServerAuthenticatorTest extends CategoryTest {
  private static final String GLOBAL_ACCOUNT_ID = "__GLOBAL_ACCOUNT_ID__";
  private static final String ACCOUNT_ID = "ACCOUNT_ID";

  @Mock LoadingCache<String, String> keyCache;
  @Mock LoadingCache<String, DelegateTokenStatus> defaultTokenStatusCache;
  @Mock private HPersistence persistence;
  @Inject @InjectMocks private DelegateTokenEventServerAuthenticatorImpl delegateTokenAuthenticator;

  private String accountKey = "2f6b0988b6fb3370073c3d0505baee59";

  @Before
  public void setUp() throws Exception {
    initMocks(this);
    FieldUtils.writeField(delegateTokenAuthenticator, "keyCache", keyCache, true);
    FieldUtils.writeField(delegateTokenAuthenticator, "defaultTokenStatusCache", defaultTokenStatusCache, true);
    when(keyCache.get(ACCOUNT_ID)).thenReturn(accountKey);
    when(defaultTokenStatusCache.get(ACCOUNT_ID)).thenReturn(DelegateTokenStatus.ACTIVE);
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
    when(defaultTokenStatusCache.get(ACCOUNT_ID)).thenReturn(DelegateTokenStatus.REVOKED);

    DelegateToken delegateToken = DelegateToken.builder()
                                      .accountId(ACCOUNT_ID)
                                      .name("custom")
                                      .value(accountKey)
                                      .status(DelegateTokenStatus.ACTIVE)
                                      .build();

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

    assertThatCode(()
                       -> delegateTokenAuthenticator.validateDelegateToken(
                           ACCOUNT_ID, tokenGenerator.getToken("https", "localhost", 9090, "hostname")))
        .doesNotThrowAnyException();
    verify(persistence).createQuery(DelegateToken.class);
  }

  @Test
  @Owner(developers = LUCAS)
  @Category(UnitTests.class)
  public void shouldValidateDelegateTokenThrowsInvalidTokenException() {
    when(defaultTokenStatusCache.get(ACCOUNT_ID)).thenReturn(DelegateTokenStatus.REVOKED);

    DelegateToken delegateToken = DelegateToken.builder()
                                      .accountId(ACCOUNT_ID)
                                      .name("custom")
                                      .value(accountKey)
                                      .status(DelegateTokenStatus.REVOKED)
                                      .build();

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
    when(defaultTokenStatusCache.get(ACCOUNT_ID)).thenReturn(DelegateTokenStatus.REVOKED);

    DelegateToken delegateTokenRevoked = DelegateToken.builder()
                                             .accountId(ACCOUNT_ID)
                                             .name("TokenName")
                                             .value(accountKey)
                                             .status(DelegateTokenStatus.REVOKED)
                                             .build();

    Query mockQuery = mock(Query.class);
    FieldEnd<Service> fieldEnd = mock(FieldEnd.class);

    MorphiaIterator<DelegateToken, DelegateToken> morphiaIterator = mock(MorphiaIterator.class);

    doReturn(mockQuery).when(persistence).createQuery(DelegateToken.class);
    doReturn(fieldEnd).when(mockQuery).field(anyString());
    doReturn(mockQuery).when(fieldEnd).equal(any());

    when(morphiaIterator.hasNext())
        .thenReturn(false)
        .thenReturn(true)
        .thenReturn(false); // Return no ACTIVE tokens and then return one revoked
    when(morphiaIterator.next()).thenReturn(delegateTokenRevoked);
    when(mockQuery.fetch()).thenReturn(morphiaIterator);

    TokenGenerator tokenGenerator = new TokenGenerator(ACCOUNT_ID, accountKey);

    assertThatThrownBy(()
                           -> delegateTokenAuthenticator.validateDelegateToken(
                               ACCOUNT_ID, tokenGenerator.getToken("https", "localhost", 9090, "hostname")))
        .isInstanceOf(RevokedTokenException.class);
  }

  @Test
  @Owner(developers = LUCAS)
  @Category(UnitTests.class)
  public void shouldValidateDelegateToken_FailToDecrypt() {
    when(defaultTokenStatusCache.get(ACCOUNT_ID)).thenReturn(DelegateTokenStatus.REVOKED);

    DelegateToken delegateTokenActive = DelegateToken.builder()
                                            .accountId(ACCOUNT_ID)
                                            .name("TokenName")
                                            .value("InvalidTokenValue")
                                            .status(DelegateTokenStatus.ACTIVE)
                                            .build();

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
    Query mockQuery = mock(Query.class);
    FieldEnd<Service> fieldEnd = mock(FieldEnd.class);

    MorphiaIterator<DelegateToken, DelegateToken> morphiaIterator = mock(MorphiaIterator.class);

    doReturn(mockQuery).when(persistence).createQuery(DelegateToken.class);
    doReturn(fieldEnd).when(mockQuery).field(anyString());
    doReturn(mockQuery).when(fieldEnd).equal(any());

    when(morphiaIterator.hasNext()).thenReturn(false);
    when(mockQuery.fetch()).thenReturn(morphiaIterator);

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
