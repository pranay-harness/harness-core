package io.harness.ccm.config;

import static io.harness.rule.OwnerRule.HANTANG;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.ccm.config.GcpBillingAccount.GcpBillingAccountKeys;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;

import java.util.List;

public class GcpBillingAccountDaoTest extends WingsBaseTest {
  private String accountId = "ACCOUNT_ID";
  private String gcpOrganizationUuid = "1";
  private GcpBillingAccount gcpBillingAccount1;
  private GcpBillingAccount gcpBillingAccount2;
  @Inject private GcpOrganizationDao gcpOrganizationDao;
  @Inject private GcpBillingAccountDao gcpBillingAccountDao;

  @Before
  public void setUp() {
    gcpBillingAccount1 =
        GcpBillingAccount.builder().accountId(accountId).organizationSettingId(gcpOrganizationUuid).build();
    gcpBillingAccount2 = GcpBillingAccount.builder()
                             .accountId(accountId)
                             .organizationSettingId(gcpOrganizationUuid)
                             .exportEnabled(true)
                             .build();
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void testGet() {
    String uuid = gcpBillingAccountDao.save(gcpBillingAccount1);
    GcpBillingAccount gcpBillingAccount = gcpBillingAccountDao.get(uuid);
    assertThat(gcpBillingAccount.getUuid()).isEqualTo(uuid);
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void testList() {
    gcpBillingAccountDao.save(gcpBillingAccount1);
    gcpBillingAccountDao.save(gcpBillingAccount2);
    List<GcpBillingAccount> billingAccounts = gcpBillingAccountDao.list(accountId, null);
    assertThat(billingAccounts).hasSize(2);
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void shouldDelete() {
    GcpOrganization upsertedGcpOrganization = gcpOrganizationDao.upsert(GcpOrganization.builder().build());
    gcpBillingAccount1 = GcpBillingAccount.builder()
                             .accountId(accountId)
                             .organizationSettingId(upsertedGcpOrganization.getUuid())
                             .build();
    GcpBillingAccount upsertedGcpBillingAccount = gcpBillingAccountDao.upsert(gcpBillingAccount1);
    boolean result =
        gcpBillingAccountDao.delete(accountId, upsertedGcpOrganization.getUuid(), upsertedGcpBillingAccount.getUuid());
    assertThat(result).isTrue();
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void shouldUpsert() {
    GcpBillingAccount actualGcpBillingAccount1 = gcpBillingAccountDao.upsert(gcpBillingAccount1);
    assertThat(actualGcpBillingAccount1)
        .isEqualToIgnoringGivenFields(gcpBillingAccount1, GcpBillingAccountKeys.uuid, GcpBillingAccountKeys.createdAt,
            GcpBillingAccountKeys.lastUpdatedAt);
    GcpBillingAccount actualGcpBillingAccount2 = gcpBillingAccountDao.upsert(gcpBillingAccount2);
    assertThat(actualGcpBillingAccount2.getUuid()).isEqualTo(actualGcpBillingAccount1.getUuid());
    assertThat(actualGcpBillingAccount2)
        .isEqualToIgnoringGivenFields(gcpBillingAccount2, GcpBillingAccountKeys.uuid, GcpBillingAccountKeys.createdAt,
            GcpBillingAccountKeys.lastUpdatedAt);
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void testUpdate() {
    String billingAccountId1 = gcpBillingAccountDao.save(gcpBillingAccount1);
    gcpBillingAccountDao.update(billingAccountId1, gcpBillingAccount2);
    assertThat(gcpBillingAccountDao.get(billingAccountId1).isExportEnabled()).isEqualTo(true);
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void testSave() {
    String uuid = gcpBillingAccountDao.save(gcpBillingAccount1);
    assertThat(uuid).isNotNull();
  }
}
