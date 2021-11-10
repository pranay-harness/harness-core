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

  @Test
  @Owner(developers = VLAD)
  @Category(UnitTests.class)
  public void shouldListTokens() {
    String tokenName1 = "token1";
    String tokenName2 = "token2";
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
}
