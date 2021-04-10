package io.harness.service;

import static io.harness.rule.OwnerRule.LUCAS;
import static io.harness.rule.OwnerRule.NICOLAS;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.DelegateServiceTestBase;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.DelegateToken;
import io.harness.delegate.beans.DelegateToken.DelegateTokenKeys;
import io.harness.delegate.beans.DelegateTokenDetails;
import io.harness.delegate.beans.DelegateTokenStatus;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;
import io.harness.service.intfc.DelegateTokenService;

import com.google.inject.Inject;
import com.mongodb.DuplicateKeyException;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(HarnessTeam.DEL)
public class DelegateTokenServiceTest extends DelegateServiceTestBase {
  private static final String TEST_ACCOUNT_ID = "testAccountId";
  private static final String TEST_ACCOUNT_ID_2 = "testAccountId2";
  private static final String TEST_TOKEN_NAME = "testTokenName";
  private static final String TEST_TOKEN_NAME2 = "testTokenName2";

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
    DelegateTokenDetails createdToken = delegateTokenService.createDelegateToken(TEST_ACCOUNT_ID, TEST_TOKEN_NAME);
    assertCreatedToken(createdToken);

    DelegateTokenDetails retrievedToken = retrieveTokenFromDB(TEST_TOKEN_NAME);
    assertCreatedToken(retrievedToken);
  }

  @Test(expected = DuplicateKeyException.class)
  @Owner(developers = NICOLAS)
  @Category(UnitTests.class)
  public void testDelegateTokenServiceCreateTokenInvalidDuplicate() {
    DelegateTokenDetails createdToken = delegateTokenService.createDelegateToken(TEST_ACCOUNT_ID, TEST_TOKEN_NAME);
    assertCreatedToken(createdToken);

    delegateTokenService.createDelegateToken(TEST_ACCOUNT_ID, TEST_TOKEN_NAME);
  }

  @Test
  @Owner(developers = NICOLAS)
  @Category(UnitTests.class)
  public void testDelegateTokenServiceCreateTokenValidDuplicate() {
    DelegateTokenDetails createdToken = delegateTokenService.createDelegateToken(TEST_ACCOUNT_ID, TEST_TOKEN_NAME);
    assertCreatedToken(createdToken);

    DelegateTokenDetails tokenForOtherAccount =
        delegateTokenService.createDelegateToken(TEST_ACCOUNT_ID_2, TEST_TOKEN_NAME);
    assertThat(tokenForOtherAccount).isNotNull();
    assertThat(tokenForOtherAccount.getAccountId()).isEqualTo(TEST_ACCOUNT_ID_2);
    assertThat(tokenForOtherAccount.getName()).isEqualTo(TEST_TOKEN_NAME);
    assertThat(tokenForOtherAccount.getStatus()).isEqualTo(DelegateTokenStatus.ACTIVE);
    assertThat(tokenForOtherAccount.getUuid()).isNotEmpty();
    assertThat(tokenForOtherAccount.getUuid()).isNotEqualTo(createdToken.getUuid());
  }

  @Test
  @Owner(developers = NICOLAS)
  @Category(UnitTests.class)
  public void testDelegateTokenServiceRevokeToken() {
    DelegateTokenDetails createdToken = delegateTokenService.createDelegateToken(TEST_ACCOUNT_ID, TEST_TOKEN_NAME);
    assertCreatedToken(createdToken);

    delegateTokenService.revokeDelegateToken(createdToken.getAccountId(), createdToken.getName());

    DelegateTokenDetails retrievedToken = retrieveTokenFromDB(createdToken.getName());
    assertThat(retrievedToken).isNotNull();
    assertThat(retrievedToken.getUuid()).isEqualTo(createdToken.getUuid());
    assertThat(retrievedToken.getStatus()).isEqualTo(DelegateTokenStatus.REVOKED);
  }

  @Test
  @Owner(developers = NICOLAS)
  @Category(UnitTests.class)
  public void testDelegateTokenServiceDeleteToken() {
    DelegateTokenDetails createdToken = delegateTokenService.createDelegateToken(TEST_ACCOUNT_ID, TEST_TOKEN_NAME);
    assertCreatedToken(createdToken);

    delegateTokenService.deleteDelegateToken(TEST_ACCOUNT_ID, TEST_TOKEN_NAME);

    DelegateTokenDetails delegateToken = retrieveTokenFromDB(TEST_TOKEN_NAME);
    assertThat(delegateToken).isNull();
  }

  @Test
  @Owner(developers = LUCAS)
  @Category(UnitTests.class)
  public void testGetAllDelegateTokens() {
    DelegateTokenDetails createdToken = delegateTokenService.createDelegateToken(TEST_ACCOUNT_ID, TEST_TOKEN_NAME);
    List<DelegateTokenDetails> delegateTokens =
        delegateTokenService.getDelegateTokens(createdToken.getAccountId(), null, null);

    assertThat(delegateTokens).isNotNull();
    assertThat(delegateTokens.get(0).getAccountId()).isEqualTo(TEST_ACCOUNT_ID);
    assertThat(delegateTokens.get(0).getName()).isEqualTo(TEST_TOKEN_NAME);
    assertThat(delegateTokens.get(0).getStatus()).isEqualTo(DelegateTokenStatus.ACTIVE);
  }

  @Test
  @Owner(developers = LUCAS)
  @Category(UnitTests.class)
  public void testGetDelegateTokensByStatus() {
    delegateTokenService.createDelegateToken(TEST_ACCOUNT_ID, TEST_TOKEN_NAME);
    delegateTokenService.createDelegateToken(TEST_ACCOUNT_ID, TEST_TOKEN_NAME2);

    delegateTokenService.revokeDelegateToken(TEST_ACCOUNT_ID, TEST_TOKEN_NAME2);

    List<DelegateTokenDetails> delegateTokens =
        delegateTokenService.getDelegateTokens(TEST_ACCOUNT_ID, DelegateTokenStatus.REVOKED.name(), null);

    assertThat(delegateTokens).isNotNull();
    assertThat(delegateTokens.size()).isEqualTo(1);
    assertThat(delegateTokens.get(0).getAccountId()).isEqualTo(TEST_ACCOUNT_ID);
    assertThat(delegateTokens.get(0).getName()).isEqualTo(TEST_TOKEN_NAME2);
    assertThat(delegateTokens.get(0).getStatus()).isEqualTo(DelegateTokenStatus.REVOKED);
  }

  @Test
  @Owner(developers = LUCAS)
  @Category(UnitTests.class)
  public void testGetDelegateTokensByName() {
    delegateTokenService.createDelegateToken(TEST_ACCOUNT_ID, TEST_TOKEN_NAME);
    delegateTokenService.createDelegateToken(TEST_ACCOUNT_ID, TEST_TOKEN_NAME2);

    List<DelegateTokenDetails> delegateTokens =
        delegateTokenService.getDelegateTokens(TEST_ACCOUNT_ID, null, TEST_TOKEN_NAME2);

    assertThat(delegateTokens).isNotNull();
    assertThat(delegateTokens.size()).isEqualTo(1);
    assertThat(delegateTokens.get(0).getAccountId()).isEqualTo(TEST_ACCOUNT_ID);
    assertThat(delegateTokens.get(0).getName()).isEqualTo(TEST_TOKEN_NAME2);
    assertThat(delegateTokens.get(0).getStatus()).isEqualTo(DelegateTokenStatus.ACTIVE);
  }

  private DelegateTokenDetails retrieveTokenFromDB(String tokenName) {
    DelegateToken delegateToken =
        persistence.createQuery(DelegateToken.class).field(DelegateTokenKeys.name).equal(tokenName).get();

    return delegateToken != null ? getDelegateTokenDetails(delegateToken) : null;
  }

  private void assertCreatedToken(DelegateTokenDetails tokenToAssert) {
    assertThat(tokenToAssert).isNotNull();
    assertThat(tokenToAssert.getAccountId()).isEqualTo(TEST_ACCOUNT_ID);
    assertThat(tokenToAssert.getName()).isEqualTo(TEST_TOKEN_NAME);
    assertThat(tokenToAssert.getStatus()).isEqualTo(DelegateTokenStatus.ACTIVE);
    assertThat(tokenToAssert.getUuid()).isNotEmpty();
  }

  private DelegateTokenDetails getDelegateTokenDetails(DelegateToken delegateToken) {
    return DelegateTokenDetails.builder()
        .uuid(delegateToken.getUuid())
        .accountId(delegateToken.getAccountId())
        .name(delegateToken.getName())
        .createdAt(delegateToken.getCreatedAt())
        .createdBy(delegateToken.getCreatedBy())
        .status(delegateToken.getStatus())
        .build();
  }
}
