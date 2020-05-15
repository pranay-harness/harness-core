package io.harness.ccm.setup.service.impl;

import com.google.inject.Inject;

import com.amazonaws.services.organizations.model.Account;
import io.harness.ccm.setup.CECloudAccountDao;
import io.harness.ccm.setup.service.intfc.AWSAccountService;
import io.harness.ccm.setup.service.support.intfc.AWSOrganizationHelperService;
import io.harness.ccm.setup.util.InfraSetUpUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import software.wings.app.MainConfiguration;
import software.wings.beans.AwsCrossAccountAttributes;
import software.wings.beans.SettingAttribute;
import software.wings.beans.ce.CEAwsConfig;
import software.wings.beans.ce.CECloudAccount;
import software.wings.beans.ce.CECloudAccount.AccountStatus;
import software.wings.service.intfc.SettingsService;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class AWSAccountServiceImpl implements AWSAccountService {
  private final MainConfiguration configuration;
  private final SettingsService settingsService;
  private final CECloudAccountDao ceCloudAccountDao;
  private final AwsCEInfraSetupHandler awsCEInfraSetupHandler;
  private final AWSOrganizationHelperService awsOrganizationHelperService;

  @Inject
  public AWSAccountServiceImpl(AWSOrganizationHelperService awsOrganizationHelperService,
      SettingsService settingsService, CECloudAccountDao ceCloudAccountDao,
      AwsCEInfraSetupHandler awsCEInfraSetupHandler, MainConfiguration configuration) {
    this.awsOrganizationHelperService = awsOrganizationHelperService;
    this.settingsService = settingsService;
    this.ceCloudAccountDao = ceCloudAccountDao;
    this.awsCEInfraSetupHandler = awsCEInfraSetupHandler;
    this.configuration = configuration;
  }

  @Override
  public List<CECloudAccount> getAWSAccounts(String accountId, String settingId, CEAwsConfig ceAwsConfig) {
    List<CECloudAccount> ceCloudAccounts = new ArrayList<>();
    List<Account> accountList =
        awsOrganizationHelperService.listAwsAccounts(ceAwsConfig.getAwsCrossAccountAttributes());
    String masterAwsAccountId =
        getMasterAccountIdFromArn(ceAwsConfig.getAwsCrossAccountAttributes().getCrossAccountRoleArn());
    String externalId = ceAwsConfig.getAwsCrossAccountAttributes().getExternalId();
    accountList.forEach(account -> {
      String awsAccountId = getAccountIdFromArn(account.getArn());
      if (!awsAccountId.equals(masterAwsAccountId)) {
        AwsCrossAccountAttributes linkedAwsCrossAccountAttributes =
            AwsCrossAccountAttributes.builder()
                .externalId(externalId)
                .crossAccountRoleArn(InfraSetUpUtils.getLinkedAccountArn(
                    awsAccountId, configuration.getCeSetUpConfig().getAwsRoleName()))
                .build();
        CECloudAccount cloudAccount = CECloudAccount.builder()
                                          .accountId(accountId)
                                          .accountArn(account.getArn())
                                          .accountStatus(AccountStatus.NOT_VERIFIED)
                                          .accountName(account.getName())
                                          .infraAccountId(awsAccountId)
                                          .infraMasterAccountId(ceAwsConfig.getAwsMasterAccountId())
                                          .masterAccountSettingId(settingId)
                                          .awsCrossAccountAttributes(linkedAwsCrossAccountAttributes)
                                          .build();
        ceCloudAccounts.add(cloudAccount);
      }
    });
    logger.info("CE cloud account {}", ceCloudAccounts);
    return ceCloudAccounts;
  }

  @Override
  public void updateAccountPermission(String accountId, String settingId) {
    updateAccountPermission(settingsService.getById(accountId, settingId));
  }

  @Override
  public void updateAccountPermission(SettingAttribute settingAttribute) {
    if (null != settingAttribute) {
      if (settingAttribute.getValue() instanceof CEAwsConfig) {
        CEAwsConfig ceAwsConfig = (CEAwsConfig) settingAttribute.getValue();
        List<CECloudAccount> ceCloudAccounts = ceCloudAccountDao.getByMasterAccountId(
            settingAttribute.getAccountId(), settingAttribute.getUuid(), ceAwsConfig.getAwsAccountId());
        ceCloudAccounts.forEach(awsCEInfraSetupHandler::updateAccountPermission);
      }
    }
  }

  public static String getAccountIdFromArn(String arn) {
    return StringUtils.substringAfterLast(arn, "/");
  }

  public static String getMasterAccountIdFromArn(String arn) {
    return StringUtils.substringBefore(StringUtils.substringAfterLast(arn, "iam::"), ":");
  }
}
