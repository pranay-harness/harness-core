package io.harness.ccm.config;

import com.google.inject.Inject;

import java.util.List;

public class GcpBillingAccountServiceImpl implements GcpBillingAccountService {
  @Inject private GcpBillingAccountDao gcpBillingAccountDao;

  @Override
  public String create(GcpBillingAccount billingAccount) {
    return gcpBillingAccountDao.save(billingAccount);
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
