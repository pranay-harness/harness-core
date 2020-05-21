package io.harness.ccm.config;

import static java.lang.String.format;

import com.google.cloud.bigquery.BigQuery;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.ccm.billing.GcpServiceAccountService;
import io.harness.ccm.billing.bigquery.BigQueryService;
import io.harness.exception.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;
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
  public GcpBillingAccount create(GcpBillingAccount billingAccount) {
    GcpOrganization gcpOrganization = gcpOrganizationService.get(billingAccount.getOrganizationSettingId());
    if (null == gcpOrganization) {
      throw new InvalidRequestException(
          "A valid GCP organization information should be provided for the billing account.");
    }
    String impersonatedServiceAccount = gcpOrganization.getServiceAccountEmail();
    logger.info(format(
        "Validating access to GCP billing report by impersonating the service account %s", impersonatedServiceAccount));
    ValidationResult result = validateAccessToBillingReport(billingAccount, impersonatedServiceAccount);
    if (result.isValid()) {
      return gcpBillingAccountDao.upsert(billingAccount);
    } else {
      throw new InvalidRequestException(result.getErrorMessage());
    }
  }

  @Override
  public ValidationResult validateAccessToBillingReport(
      GcpBillingAccount gcpBillingAccount, String impersonatedServiceAccount) {
    String bqProjectId = gcpBillingAccount.getBqProjectId();
    String bqDatasetId = gcpBillingAccount.getBqDatasetId();
    BigQuery bigQuery = bigQueryService.get(bqProjectId, impersonatedServiceAccount);
    return bigQueryService.canAccessDataset(bigQuery, bqProjectId, bqDatasetId);
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
