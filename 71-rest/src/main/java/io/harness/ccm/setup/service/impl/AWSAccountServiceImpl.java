package io.harness.ccm.setup.service.impl;

import com.google.inject.Inject;

import com.amazonaws.services.organizations.model.Account;
import io.harness.ccm.setup.service.intfc.AWSAccountService;
import io.harness.ccm.setup.service.support.intfc.AWSOrganizationHelperService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import software.wings.beans.ce.CEAwsConfig;
import software.wings.beans.ce.CECloudAccount;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class AWSAccountServiceImpl implements AWSAccountService {
  private final AWSOrganizationHelperService awsOrganizationHelperService;

  @Inject
  public AWSAccountServiceImpl(AWSOrganizationHelperService awsOrganizationHelperService) {
    this.awsOrganizationHelperService = awsOrganizationHelperService;
  }

  @Override
  public List<CECloudAccount> getAWSAccounts(String accountId, String settingId, CEAwsConfig ceAwsConfig) {
    List<CECloudAccount> ceCloudAccounts = new ArrayList<>();
    List<Account> accountList =
        awsOrganizationHelperService.listAwsAccounts(ceAwsConfig.getAwsCrossAccountAttributes());
    String masterAwsAccountId =
        getMasterAccountIdFromArn(ceAwsConfig.getAwsCrossAccountAttributes().getCrossAccountRoleArn());
    accountList.forEach(account -> {
      String awsAccountId = getAccountIdFromArn(account.getArn());
      if (!awsAccountId.equals(masterAwsAccountId)) {
        CECloudAccount cloudAccount = CECloudAccount.builder()
                                          .accountId(accountId)
                                          .accountArn(account.getArn())
                                          .accountName(account.getName())
                                          .infraAccountId(awsAccountId)
                                          .infraMasterAccountId(ceAwsConfig.getAwsMasterAccountId())
                                          .masterAccountSettingId(settingId)
                                          .build();
        ceCloudAccounts.add(cloudAccount);
      }
    });
    logger.info("CE cloud account {}", ceCloudAccounts);
    return ceCloudAccounts;
  }

  public static String getAccountIdFromArn(String arn) {
    return StringUtils.substringAfterLast(arn, "/");
  }

  public static String getMasterAccountIdFromArn(String arn) {
    return StringUtils.substringBefore(StringUtils.substringAfterLast(arn, "iam::"), ":");
  }
}
