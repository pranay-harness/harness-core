/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.service;

import static io.harness.rule.OwnerRule.VLAD;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.MockitoAnnotations.initMocks;

import io.harness.DelegateServiceTestBase;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.DelegateTokenDetails;
import io.harness.delegate.beans.DelegateTokenStatus;
import io.harness.exception.InvalidRequestException;
import io.harness.outbox.api.OutboxService;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;
import io.harness.service.impl.DelegateNgTokenServiceImpl;
import io.harness.service.intfc.DelegateNgTokenService;

import com.google.inject.Inject;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;

@OwnedBy(HarnessTeam.DEL)
public class DelegateNgTokenServiceTest extends DelegateServiceTestBase {
  private static final String TEST_ACCOUNT_ID = "testAccountId";

  @Inject private HPersistence persistence;
  @Inject private OutboxService outboxService;
  @InjectMocks @Inject private DelegateNgTokenServiceImpl delegateNgTokenService;

  @Before
  public void setUp() {
    initMocks(this);
  }

  @Test
  @Owner(developers = VLAD)
  @Category(UnitTests.class)
  public void shouldCreateToken() {
    String tokenName = "token1";
    DelegateTokenDetails token = delegateNgTokenService.createToken(TEST_ACCOUNT_ID, null, tokenName);
    assertThat(token.getName()).isEqualTo(tokenName);
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = VLAD)
  @Category(UnitTests.class)
  public void shouldNotCreateWhenDuplicatedName() {
    String tokenName = "token1";
    delegateNgTokenService.createToken(TEST_ACCOUNT_ID, null, tokenName);
    delegateNgTokenService.createToken(TEST_ACCOUNT_ID, null, tokenName);
  }

  @Test
  @Owner(developers = VLAD)
  @Category(UnitTests.class)
  public void shouldRevokeToken() {
    String tokenName = "token1";
    DelegateTokenDetails token = delegateNgTokenService.createToken(TEST_ACCOUNT_ID, null, tokenName);
    assertThat(delegateNgTokenService.getDelegateTokens(TEST_ACCOUNT_ID, null, DelegateTokenStatus.ACTIVE)).hasSize(1);
    DelegateTokenDetails revokedToken = delegateNgTokenService.revokeDelegateToken(TEST_ACCOUNT_ID, null, tokenName);
    assertThat(revokedToken.getStatus()).isEqualTo(DelegateTokenStatus.REVOKED);
    assertThat(delegateNgTokenService.getDelegateTokens(TEST_ACCOUNT_ID, null, DelegateTokenStatus.ACTIVE)).isEmpty();
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = VLAD)
  @Category(UnitTests.class)
  public void shouldNotRevokeAlreadyRevokedToken() {
    String tokenName = "token1";
    delegateNgTokenService.createToken(TEST_ACCOUNT_ID, null, tokenName);
    assertThat(delegateNgTokenService.getDelegateTokens(TEST_ACCOUNT_ID, null, DelegateTokenStatus.ACTIVE)).hasSize(1);
    delegateNgTokenService.revokeDelegateToken(TEST_ACCOUNT_ID, null, tokenName);
    assertThat(delegateNgTokenService.getDelegateTokens(TEST_ACCOUNT_ID, null, DelegateTokenStatus.ACTIVE)).isEmpty();
    delegateNgTokenService.revokeDelegateToken(TEST_ACCOUNT_ID, null, tokenName);
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = VLAD)
  @Category(UnitTests.class)
  public void shouldNotCreateTokenWithTheSameNameAsRevokedToken() {
    String tokenName = "token1";
    delegateNgTokenService.createToken(TEST_ACCOUNT_ID, null, tokenName);
    assertThat(delegateNgTokenService.getDelegateTokens(TEST_ACCOUNT_ID, null, DelegateTokenStatus.ACTIVE)).hasSize(1);
    delegateNgTokenService.revokeDelegateToken(TEST_ACCOUNT_ID, null, tokenName);
    assertThat(delegateNgTokenService.getDelegateTokens(TEST_ACCOUNT_ID, null, DelegateTokenStatus.ACTIVE)).isEmpty();
    delegateNgTokenService.createToken(TEST_ACCOUNT_ID, null, tokenName);
  }

  @Test
  @Owner(developers = VLAD)
  @Category(UnitTests.class)
  public void shouldListTokens() {
    String tokenName1 = "token1";
    String tokenName2 = "token12";
    delegateNgTokenService.createToken(TEST_ACCOUNT_ID, null, tokenName1);
    delegateNgTokenService.createToken(TEST_ACCOUNT_ID, null, tokenName2);
    assertThat(delegateNgTokenService.getDelegateTokens(TEST_ACCOUNT_ID, null, DelegateTokenStatus.ACTIVE)).hasSize(2);
    delegateNgTokenService.revokeDelegateToken(TEST_ACCOUNT_ID, null, tokenName1);
    assertThat(delegateNgTokenService.getDelegateTokens(TEST_ACCOUNT_ID, null, DelegateTokenStatus.ACTIVE)).hasSize(1);
    assertThat(delegateNgTokenService.getDelegateTokens(TEST_ACCOUNT_ID, null, DelegateTokenStatus.REVOKED)).hasSize(1);
    delegateNgTokenService.revokeDelegateToken(TEST_ACCOUNT_ID, null, tokenName2);
    assertThat(delegateNgTokenService.getDelegateTokens(TEST_ACCOUNT_ID, null, DelegateTokenStatus.ACTIVE)).hasSize(0);
    assertThat(delegateNgTokenService.getDelegateTokens(TEST_ACCOUNT_ID, null, DelegateTokenStatus.REVOKED)).hasSize(2);
  }

  @Test
  @Owner(developers = VLAD)
  @Category(UnitTests.class)
  public void shouldGetToken() {
    String tokenName1 = "token1";
    String tokenName2 = "token12";
    delegateNgTokenService.createToken(TEST_ACCOUNT_ID, null, tokenName1);
    delegateNgTokenService.createToken(TEST_ACCOUNT_ID, null, tokenName2);
    assertThat(delegateNgTokenService.getDelegateTokens(TEST_ACCOUNT_ID, null, DelegateTokenStatus.ACTIVE)).hasSize(2);
    DelegateTokenDetails result = delegateNgTokenService.getDelegateToken(TEST_ACCOUNT_ID, null, tokenName1);
    assertThat(result.getName()).isEqualTo(tokenName1);
  }

  @Test
  @Owner(developers = VLAD)
  @Category(UnitTests.class)
  public void shouldGetTokenValue() {
    String tokenName1 = "token1";
    String tokenName2 = "token12";
    DelegateTokenDetails delegateTokenDetails = delegateNgTokenService.createToken(TEST_ACCOUNT_ID, null, tokenName1);
    delegateNgTokenService.createToken(TEST_ACCOUNT_ID, null, tokenName2);
    String result = delegateNgTokenService.getDelegateTokenValue(TEST_ACCOUNT_ID, null, tokenName1);
    assertThat(result).isEqualTo(delegateTokenDetails.getValue());
  }

  @Test
  @Owner(developers = VLAD)
  @Category(UnitTests.class)
  public void shouldUpsertDefaultToken() {
    String tokenName1 = DelegateNgTokenService.DEFAULT_TOKEN_NAME;
    DelegateTokenDetails delegateTokenDetails = delegateNgTokenService.createToken(TEST_ACCOUNT_ID, null, tokenName1);
    DelegateTokenDetails result = delegateNgTokenService.upsertDefaultToken(TEST_ACCOUNT_ID, null, false);
    assertThat(result.getValue()).isNotEqualTo(delegateTokenDetails.getValue());
  }

  @Test
  @Owner(developers = VLAD)
  @Category(UnitTests.class)
  public void shouldUpsertDefaultTokenSkipIfExists() {
    String tokenName1 = DelegateNgTokenService.DEFAULT_TOKEN_NAME;
    DelegateTokenDetails delegateTokenDetails = delegateNgTokenService.createToken(TEST_ACCOUNT_ID, null, tokenName1);
    DelegateTokenDetails result = delegateNgTokenService.upsertDefaultToken(TEST_ACCOUNT_ID, null, true);
    assertThat(result.getValue()).isEqualTo(delegateTokenDetails.getValue());
  }
}
