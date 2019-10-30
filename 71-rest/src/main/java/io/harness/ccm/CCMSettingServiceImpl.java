package io.harness.ccm;

import static java.util.Objects.isNull;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.ccm.cluster.entities.ClusterRecord;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Account;
import software.wings.beans.SettingAttribute;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.SettingsService;
import software.wings.settings.SettingValue;

@Slf4j
@Singleton
public class CCMSettingServiceImpl implements CCMSettingService {
  private AccountService accountService;
  private SettingsService settingsService;

  @Inject
  public CCMSettingServiceImpl(AccountService accountService, SettingsService settingsService) {
    this.accountService = accountService;
    this.settingsService = settingsService;
  }

  @Override
  public boolean isCloudCostEnabled(SettingAttribute settingAttribute) {
    Account account = accountService.get(settingAttribute.getAccountId());
    if (account.isCloudCostEnabled()) {
      CloudCostAware value = (CloudCostAware) settingAttribute.getValue();
      CCMConfig ccmConfig = value.getCcmConfig();
      if (!isNull(ccmConfig)) {
        return ccmConfig.isCloudCostEnabled();
      }
    }
    return false;
  }

  @Override
  public SettingAttribute maskCCMConfig(SettingAttribute settingAttribute) {
    Account account = accountService.get(settingAttribute.getAccountId());
    if (!account.isCloudCostEnabled()) {
      CloudCostAware value = (CloudCostAware) settingAttribute.getValue();
      value.setCcmConfig(null);
      settingAttribute.setValue((SettingValue) value);
    }
    return settingAttribute;
  }

  @Override
  public boolean isCloudCostEnabled(ClusterRecord clusterRecord) {
    String cloudProviderId = clusterRecord.getCluster().getCloudProviderId();
    SettingAttribute settingAttribute = settingsService.get(cloudProviderId);
    if (isNull(settingAttribute)) {
      logger.error("Failed to find the Cloud Provider associated with the Cluster with id={}", clusterRecord.getUuid());
      return false;
    }
    if (settingAttribute.getValue() instanceof CloudCostAware) {
      if (isCloudCostEnabled(settingAttribute)) {
        return true;
      }
    }
    return false;
  }
}
