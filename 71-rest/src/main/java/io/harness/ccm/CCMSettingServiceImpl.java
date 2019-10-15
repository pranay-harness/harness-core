package io.harness.ccm;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.beans.Account;
import software.wings.beans.SettingAttribute;
import software.wings.service.intfc.AccountService;
import software.wings.settings.SettingValue;

@Singleton
public class CCMSettingServiceImpl implements CCMSettingService {
  private AccountService accountService;

  @Inject
  public CCMSettingServiceImpl(AccountService accountService) {
    this.accountService = accountService;
  }

  public SettingAttribute maskCCMConfig(SettingAttribute settingAttribute) {
    Account account = accountService.get(settingAttribute.getAccountId());
    if (!account.isCloudCostEnabled()) {
      CloudCostAware value = (CloudCostAware) settingAttribute.getValue();
      value.setCcmConfig(null);
      settingAttribute.setValue((SettingValue) value);
    }
    return settingAttribute;
  }
}
