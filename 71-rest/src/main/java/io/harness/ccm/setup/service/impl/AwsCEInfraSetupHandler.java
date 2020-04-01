package io.harness.ccm.setup.service.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.ccm.setup.service.CEInfraSetupHandler;
import io.harness.ccm.setup.service.intfc.AWSAccountService;
import io.harness.ccm.setup.service.intfc.AwsEKSClusterService;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.SettingAttribute;
import software.wings.beans.ce.CEAwsConfig;
import software.wings.beans.ce.CECloudAccount;
import software.wings.beans.ce.CECluster;

import java.util.List;

@Singleton
@Slf4j
public class AwsCEInfraSetupHandler extends CEInfraSetupHandler {
  @Inject private AWSAccountService awsAccountService;
  @Inject private AwsEKSClusterService awsEKSClusterService;

  @Override
  public void syncCEInfra(SettingAttribute settingAttribute) {
    String accountId = settingAttribute.getAccountId();
    String settingId = settingAttribute.getUuid();

    if (settingAttribute.getValue() instanceof CEAwsConfig) {
      CEAwsConfig ceAwsConfig = (CEAwsConfig) settingAttribute.getValue();
      if (CEAwsConfig.AWSAccountType.MASTER_ACCOUNT.name().equals(ceAwsConfig.getAwsAccountType())) {
        List<CECloudAccount> awsAccounts = awsAccountService.getAWSAccounts(accountId, settingId, ceAwsConfig);
        updateLinkedAccounts(accountId, ceAwsConfig.getAwsAccountId(), awsAccounts);
      }
      List<CECluster> eksCluster = awsEKSClusterService.getEKSCluster(accountId, settingId, ceAwsConfig);
      updateClusters(accountId, ceAwsConfig.getAwsAccountId(), eksCluster);
    }
  }
}
