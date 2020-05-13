package io.harness.ccm.config;

import static io.harness.rule.OwnerRule.HANTANG;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;

public class GcpServiceAccountDaoTest extends WingsBaseTest {
  private String accountId = "ACCOUNT_ID";
  private String serviceAccountId = "SERVICE_ACCOUNT_ID";
  private GcpServiceAccount gcpServiceAccount;
  @Inject private GcpServiceAccountDao gcpServiceAccountDao;

  @Before
  public void setUp() {
    gcpServiceAccount = GcpServiceAccount.builder().serviceAccountId(serviceAccountId).accountId(accountId).build();
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void shouldSaveAndGet() {
    gcpServiceAccountDao.save(gcpServiceAccount);
    GcpServiceAccount result = gcpServiceAccountDao.getByServiceAccountId(serviceAccountId);
    assertThat(result).isEqualToIgnoringGivenFields(gcpServiceAccount, "uuid");
  }
}
