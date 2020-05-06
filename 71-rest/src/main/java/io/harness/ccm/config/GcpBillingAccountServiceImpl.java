package io.harness.ccm.config;

import static com.google.common.base.Preconditions.checkArgument;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.cloud.bigquery.BigQuery;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.ccm.GcpServiceAccountService;
import io.harness.ccm.billing.bigquery.BigQueryService;
import lombok.extern.slf4j.Slf4j;
import org.postgresql.shaded.com.ongres.scram.common.util.Preconditions;
import software.wings.beans.ValidationResult;

import java.util.List;

@Slf4j
@Singleton
public class GcpBillingAccountServiceImpl implements GcpBillingAccountService {
  @Inject private GcpServiceAccountService gcpServiceAccountService;
  @Inject private GcpBillingAccountDao gcpBillingAccountDao;
  @Inject private GcpOrganizationService gcpOrganizationService;
  @Inject private BigQueryService bigQueryService;

  @Override
  public String create(GcpBillingAccount billingAccount) {
    checkArgument(isNotEmpty(billingAccount.getOrganizationSettingId()));
    return gcpBillingAccountDao.save(billingAccount);
  }

  @Override
  public ValidationResult validateAccessToBillingReport(GcpBillingAccount gcpBillingAccount) {
    GcpOrganization gcpOrganization = gcpOrganizationService.get(gcpBillingAccount.getOrganizationSettingId());
    Preconditions.checkNotEmpty(gcpOrganization.getServiceAccountEmail(), "Service account is missing.");
    String impersonatedServiceAccount = gcpOrganization.getServiceAccountEmail();

    String bqProjectId = gcpBillingAccount.getBqProjectId();
    String bqDatasetId = gcpBillingAccount.getBqDatasetId();

    BigQuery bigQuery = bigQueryService.get(bqProjectId, impersonatedServiceAccount);
    return ValidationResult.builder()
        .valid(bigQueryService.canAccessDataset(bigQuery, bqProjectId, bqDatasetId))
        .build();
  }

  @Override
  public GcpBillingAccount get(String uuid) {
    return gcpBillingAccountDao.get(uuid);
  }

  @Override
  public List<GcpBillingAccount> list(String accountId, String organizationSettingId) {
    return gcpBillingAccountDao.list(accountId, organizationSettingId);
  }

  @Override
  public boolean delete(String billingAccountId) {
    return gcpBillingAccountDao.delete(billingAccountId);
  }

  @Override
  public void update(String billingAccountId, GcpBillingAccount billingAccount) {
    gcpBillingAccountDao.update(billingAccountId, billingAccount);
  }
}
