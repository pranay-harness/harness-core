/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ccm.setup.service.impl;

import static io.harness.annotations.dev.HarnessTeam.CE;

import static software.wings.service.impl.aws.model.AwsConstants.AWS_DEFAULT_REGION;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.commons.entities.billing.CECloudAccount;
import io.harness.ccm.setup.service.CEInfraSetupHandler;
import io.harness.ccm.setup.service.intfc.AWSAccountService;
import io.harness.ccm.setup.service.intfc.AwsEKSClusterService;
import io.harness.ccm.setup.service.support.intfc.AwsEKSHelperService;

import software.wings.beans.SettingAttribute;
import software.wings.beans.ce.CEAwsConfig;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
@OwnedBy(CE)
public class AwsCEInfraSetupHandler extends CEInfraSetupHandler {
  @Inject private AWSAccountService awsAccountService;
  @Inject private AwsEKSHelperService awsEKSHelperService;
  @Inject private AwsEKSClusterService awsEKSClusterService;

  private static final String DEFAULT_REGION = AWS_DEFAULT_REGION;

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
