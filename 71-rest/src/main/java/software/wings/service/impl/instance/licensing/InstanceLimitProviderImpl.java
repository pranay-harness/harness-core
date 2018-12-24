package software.wings.service.impl.instance.licensing;

import com.google.inject.Inject;

import software.wings.beans.LicenseInfo;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.instance.licensing.InstanceLimitProvider;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class InstanceLimitProviderImpl implements InstanceLimitProvider {
  private AccountService accountService;
  @Inject
  public InstanceLimitProviderImpl(AccountService accountService) {
    this.accountService = accountService;
  }
  @Override
  public long getAllowedInstances(String accountId) {
    final LicenseInfo licenseInfo = accountService.get(accountId).getLicenseInfo();
    if (null == licenseInfo) {
      throw new IllegalStateException("License Information not present. Account ID: " + accountId);
    }
    // 1 license Unit = 1 instance
    return licenseInfo.getLicenseUnits();
  }
}