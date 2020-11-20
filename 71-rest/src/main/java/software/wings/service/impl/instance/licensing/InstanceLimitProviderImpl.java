package software.wings.service.impl.instance.licensing;

import com.google.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import software.wings.beans.AccountType;
import software.wings.beans.LicenseInfo;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.instance.licensing.InstanceLimitProvider;

import javax.annotation.ParametersAreNonnullByDefault;

@Slf4j
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
      log.error(
          "License Information not present. Will assume a PAID account. Update license info in DB to prevent this. accountId={}",
          accountId);
      return InstanceLimitProvider.defaults(AccountType.PAID);
    }

    // 1 license Unit = 1 instance
    return licenseInfo.getLicenseUnits();
  }
}
