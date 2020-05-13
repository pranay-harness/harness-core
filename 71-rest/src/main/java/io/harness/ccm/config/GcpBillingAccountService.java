package io.harness.ccm.config;

import software.wings.beans.ValidationResult;

import java.util.List;

public interface GcpBillingAccountService {
  ValidationResult validateAccessToBillingReport(
      GcpBillingAccount gcpBillingAccount, String impersonatedServiceAccount);
  GcpBillingAccount create(GcpBillingAccount billingAccount);
  GcpBillingAccount get(String billingAccountId);
  List<GcpBillingAccount> list(String accountId, String organizationSettingId);
  boolean delete(String billingAccountId);
  void update(String billingAccountId, GcpBillingAccount billingAccount);
}
