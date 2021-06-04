package io.harness.ccm.setup.service.intfc;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.commons.entities.billing.CECloudAccount;

import software.wings.beans.SettingAttribute;
import software.wings.beans.ce.CEAwsConfig;

import java.util.List;

@OwnedBy(CE)
public interface AWSAccountService {
  List<CECloudAccount> getAWSAccounts(String accountId, String settingId, CEAwsConfig ceAwsConfig);

  void updateAccountPermission(String accountId, String settingId);

  void updateAccountPermission(SettingAttribute settingAttribute);
}
