package io.harness.ccm.config;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.ValidationResult;

import java.util.List;

@TargetModule(Module._490_CE_COMMONS)
public interface GcpBillingAccountService {
  ValidationResult validateAccessToBillingReport(
      GcpBillingAccount gcpBillingAccount, String impersonatedServiceAccount);
  GcpBillingAccount create(GcpBillingAccount billingAccount);
  GcpBillingAccount get(String billingAccountId);
  List<GcpBillingAccount> list(String accountId, String organizationSettingId);
  boolean delete(String accountId, String organizationSettingId);
  boolean delete(String accountId, String organizationSettingId, String billingAccountId);
  void update(String billingAccountId, GcpBillingAccount billingAccount);
}
