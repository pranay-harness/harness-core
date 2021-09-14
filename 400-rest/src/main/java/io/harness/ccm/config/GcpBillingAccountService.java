/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ccm.config;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.ValidationResult;

import java.util.List;

@OwnedBy(CE)
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
