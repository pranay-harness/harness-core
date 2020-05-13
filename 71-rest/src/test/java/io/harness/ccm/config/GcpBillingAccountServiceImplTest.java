package io.harness.ccm.config;

import static io.harness.rule.OwnerRule.HANTANG;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.bigquery.BigQuery;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.ccm.GcpServiceAccountService;
import io.harness.ccm.billing.bigquery.BigQueryService;
import io.harness.ccm.config.GcpBillingAccount.GcpBillingAccountBuilder;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import software.wings.beans.ValidationResult;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class GcpBillingAccountServiceImplTest extends CategoryTest {
  private String accountId = "ACCOUNT_ID";

  private String billingAccountUuid = "BILLING_ACCOUNT_UUID";
  private String organizationSettingId = "ORGANIZATION_SETTING_ID";
  private String impersonatedServiceAccount = "impersonate-account-1@ccm-play.iam.gserviceaccount.com";
  private GcpOrganization gcpOrganization;

  private GcpBillingAccount gcpBillingAccount;
  private String bqProjectId = "BQ_PROJECT_ID";
  private String bqDatasetId = "BQ_DATASET_ID";

  @Mock private GcpBillingAccountDao gcpBillingAccountDao;
  @Mock private GcpOrganizationService gcpOrganizationService;
  @Mock private GcpServiceAccountService gcpServiceAccountService;
  @Mock private BigQueryService bigQueryService;
  @Mock private BigQuery bigQuery;
  @InjectMocks private GcpBillingAccountServiceImpl gcpBillingAccountService;
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Before
  public void setUp() throws GeneralSecurityException, IOException {
    gcpOrganization = GcpOrganization.builder().serviceAccountEmail(impersonatedServiceAccount).build();
    when(gcpOrganizationService.get(eq(organizationSettingId))).thenReturn(gcpOrganization);

    gcpBillingAccount = getGcpBillingAccount(organizationSettingId);
    when(bigQueryService.get(eq(bqProjectId), eq(impersonatedServiceAccount))).thenReturn(bigQuery);
    when(bigQueryService.canAccessDataset(eq(bigQuery), eq(bqProjectId), eq(bqDatasetId)))
        .thenReturn(ValidationResult.builder().valid(true).build());
  }

  private GcpBillingAccount getGcpBillingAccount(String organizationSettingId) {
    GcpBillingAccountBuilder builder = GcpBillingAccount.builder().bqProjectId(bqProjectId).bqDatasetId(bqDatasetId);
    if (organizationSettingId != null) {
      builder.organizationSettingId(organizationSettingId);
    }
    return builder.build();
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void shouldCreateForValidBillingAccount() {
    gcpBillingAccountService.create(gcpBillingAccount);
    verify(gcpBillingAccountDao).upsert(gcpBillingAccount);
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void shouldNotCreateForInvalidBillingAccount() {
    GcpBillingAccount gcpBillingAccount = getGcpBillingAccount(null);
    gcpBillingAccountService.create(gcpBillingAccount);
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void shouldGet() {
    gcpBillingAccountService.get(billingAccountUuid);
    verify(gcpBillingAccountDao).get(eq(billingAccountUuid));
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void testList() {
    gcpBillingAccountService.list(accountId, organizationSettingId);
    verify(gcpBillingAccountDao).list(eq(accountId), eq(organizationSettingId));
  }
}