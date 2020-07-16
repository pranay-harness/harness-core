package io.harness.ccm.setup.service.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.ccm.setup.service.CEInfraSetupHandler;
import io.harness.ccm.setup.service.intfc.AWSAccountService;
import io.harness.ccm.setup.service.intfc.AwsEKSClusterService;
import io.harness.ccm.setup.service.support.intfc.AwsEKSHelperService;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.SettingAttribute;
import software.wings.beans.ce.CEAwsConfig;
import software.wings.beans.ce.CECloudAccount;

import java.util.List;

@Singleton
@Slf4j
public class AwsCEInfraSetupHandler extends CEInfraSetupHandler {
  @Inject private AWSAccountService awsAccountService;
  @Inject private AwsEKSHelperService awsEKSHelperService;
  @Inject private AwsEKSClusterService awsEKSClusterService;

  private static final String DEFAULT_REGION = "us-east-1";

  @Override
  public void syncCEInfra(SettingAttribute settingAttribute) {
    String accountId = settingAttribute.getAccountId();
    String settingId = settingAttribute.getUuid();

    if (settingAttribute.getValue() instanceof CEAwsConfig) {
      CEAwsConfig ceAwsConfig = (CEAwsConfig) settingAttribute.getValue();
      List<CECloudAccount> awsAccounts = awsAccountService.getAWSAccounts(accountId, settingId, ceAwsConfig);
      updateLinkedAccounts(accountId, settingId, ceAwsConfig.getAwsAccountId(), awsAccounts);
    }
  }

  @Override
  public boolean updateAccountPermission(CECloudAccount ceCloudAccount) {
    boolean verifyAccess =
        awsEKSHelperService.verifyAccess(DEFAULT_REGION, ceCloudAccount.getAwsCrossAccountAttributes());
    updateAccountStatus(ceCloudAccount, verifyAccess);
    return verifyAccess;
  }
}
