package io.harness.ccm.config;

import java.util.List;

public interface GcpBillingAccountService {
  String create(GcpBillingAccount billingAccount);
  GcpBillingAccount get(String billingAccountId);
  List<GcpBillingAccount> list(String accountId, String organizationSettingId);
  boolean delete(String billingAccountId);
  void update(String billingAccountId, GcpBillingAccount billingAccount);
}
