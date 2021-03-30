package io.harness.service;

import static io.harness.rule.OwnerRule.NICOLAS;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.DelegateServiceTestBase;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.DelegateToken;
import io.harness.delegate.beans.DelegateToken.DelegateTokenKeys;
import io.harness.delegate.beans.DelegateTokenStatus;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;
import io.harness.service.intfc.DelegateTokenService;

import com.google.inject.Inject;
import com.mongodb.DuplicateKeyException;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(HarnessTeam.DEL)
public class DelegateTokenServiceTest extends DelegateServiceTestBase {
  private static final String TEST_ACCOUNT_ID = "testAccountId";
  private static final String TEST_ACCOUNT_ID_2 = "testAccountId2";
  private static final String TEST_TOKEN_NAME = "testTokenName";

  @Inject private HPersistence persistence;
  @Inject private DelegateTokenService delegateTokenService;

  @Before
  public void setUp() {
    persistence.ensureIndexForTesting(DelegateToken.class);
  }

  @Test
  @Owner(developers = NICOLAS)
  @Category(UnitTests.class)
  public void testDelegateTokenServiceCreateToken() {
    DelegateToken createdToken = delegateTokenService.createDelegateToken(TEST_ACCOUNT_ID, TEST_TOKEN_NAME);
    assertCreatedToken(createdToken);

    DelegateToken retrievedToken = retrieveTokenFromDB(TEST_TOKEN_NAME);
    assertCreatedToken(retrievedToken);
  }

  @Test(expected = DuplicateKeyException.class)
  @Owner(developers = NICOLAS)
  @Category(UnitTests.class)
  public void testDelegateTokenServiceCreateTokenInvalidDuplicate() {
    DelegateToken createdToken = delegateTokenService.createDelegateToken(TEST_ACCOUNT_ID, TEST_TOKEN_NAME);
    assertCreatedToken(createdToken);

    delegateTokenService.createDelegateToken(TEST_ACCOUNT_ID, TEST_TOKEN_NAME);
  }

  @Test
  @Owner(developers = NICOLAS)
  @Category(UnitTests.class)
  public void testDelegateTokenServiceCreateTokenValidDuplicate() {
    DelegateToken createdToken = delegateTokenService.createDelegateToken(TEST_ACCOUNT_ID, TEST_TOKEN_NAME);
    assertCreatedToken(createdToken);

    DelegateToken tokenForOtherAccount = delegateTokenService.createDelegateToken(TEST_ACCOUNT_ID_2, TEST_TOKEN_NAME);
    assertThat(tokenForOtherAccount).isNotNull();
    assertThat(tokenForOtherAccount.getAccountId()).isEqualTo(TEST_ACCOUNT_ID_2);
    assertThat(tokenForOtherAccount.getName()).isEqualTo(TEST_TOKEN_NAME);
    assertThat(tokenForOtherAccount.getStatus()).isEqualTo(DelegateTokenStatus.ACTIVE);
    assertThat(tokenForOtherAccount.getValue()).isEmpty();
    assertThat(tokenForOtherAccount.getUuid()).isNotEmpty();
    assertThat(tokenForOtherAccount.getUuid()).isNotEqualTo(createdToken.getUuid());
  }

  @Test
  @Owner(developers = NICOLAS)
  @Category(UnitTests.class)
  public void testDelegateTokenServiceRevokeToken() {
    DelegateToken createdToken = delegateTokenService.createDelegateToken(TEST_ACCOUNT_ID, TEST_TOKEN_NAME);
    assertCreatedToken(createdToken);

    delegateTokenService.revokeDelegateToken(createdToken.getAccountId(), createdToken.getName());

    DelegateToken retrievedToken = retrieveTokenFromDB(createdToken.getName());
    assertThat(retrievedToken).isNotNull();
    assertThat(retrievedToken.getUuid()).isEqualTo(createdToken.getUuid());
    assertThat(retrievedToken.getStatus()).isEqualTo(DelegateTokenStatus.REVOKED);
  }

  @Test
  @Owner(developers = NICOLAS)
  @Category(UnitTests.class)
  public void testDelegateTokenServiceDeleteToken() {
    DelegateToken createdToken = delegateTokenService.createDelegateToken(TEST_ACCOUNT_ID, TEST_TOKEN_NAME);
    assertCreatedToken(createdToken);

    delegateTokenService.deleteDelegateToken(TEST_ACCOUNT_ID, TEST_TOKEN_NAME);

    DelegateToken delegateToken = retrieveTokenFromDB(TEST_TOKEN_NAME);
    assertThat(delegateToken).isNull();
  }

  private DelegateToken retrieveTokenFromDB(String tokenName) {
    return persistence.createQuery(DelegateToken.class).field(DelegateTokenKeys.name).equal(tokenName).get();
  }

  private void assertCreatedToken(DelegateToken tokenToAssert) {
    assertThat(tokenToAssert).isNotNull();
    assertThat(tokenToAssert.getAccountId()).isEqualTo(TEST_ACCOUNT_ID);
    assertThat(tokenToAssert.getName()).isEqualTo(TEST_TOKEN_NAME);
    assertThat(tokenToAssert.getStatus()).isEqualTo(DelegateTokenStatus.ACTIVE);
    assertThat(tokenToAssert.getUuid()).isNotEmpty();
  }
}
