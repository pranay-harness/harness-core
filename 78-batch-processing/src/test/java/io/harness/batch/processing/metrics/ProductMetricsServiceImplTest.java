package io.harness.batch.processing.metrics;

import static io.harness.rule.OwnerRule.HANTANG;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.batch.processing.config.BatchMainConfig;
import io.harness.category.element.UnitTests;
import io.harness.ccm.config.GcpBillingAccount;
import io.harness.ccm.config.GcpBillingAccountDao;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;

public class ProductMetricsServiceImplTest extends WingsBaseTest {
  private String accountId = "ACCOUNT_ID";
  private String gcpOrganizationUuid = "1";

  @Mock BatchMainConfig batchMainConfig;
  @InjectMocks @Inject ProductMetricsServiceImpl productMetricsService;
  @Inject GcpBillingAccountDao gcpBillingAccountDao;

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void shouldCountGcpBillingAccounts() {
    GcpBillingAccount gcpBillingAccount =
        GcpBillingAccount.builder().accountId(accountId).organizationSettingId(gcpOrganizationUuid).build();
    gcpBillingAccountDao.upsert(gcpBillingAccount);
    long count = productMetricsService.countGcpBillingAccounts(accountId);
    assertThat(count).isEqualTo(1);
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void shouldCountAwsBillingAccounts() {
    long count = productMetricsService.countAwsBillingAccounts(accountId);
    assertThat(count).isEqualTo(0);
  }
}
