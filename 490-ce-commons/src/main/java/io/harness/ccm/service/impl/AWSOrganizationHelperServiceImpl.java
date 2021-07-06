package io.harness.ccm.service.impl;

import static software.wings.service.impl.aws.model.AwsConstants.AWS_DEFAULT_REGION;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.commons.entities.billing.CECloudAccount;
import io.harness.ccm.service.intf.AWSOrganizationHelperService;
import io.harness.delegate.beans.connector.awsconnector.CrossAccountAccessDTO;
import io.harness.delegate.beans.connector.ceawsconnector.CEAwsConnectorDTO;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.STSAssumeRoleSessionCredentialsProvider;
import com.amazonaws.services.organizations.AWSOrganizationsClient;
import com.amazonaws.services.organizations.AWSOrganizationsClientBuilder;
import com.amazonaws.services.organizations.model.Account;
import com.amazonaws.services.organizations.model.ListAccountsRequest;
import com.amazonaws.services.organizations.model.ListAccountsResult;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.google.common.annotations.VisibleForTesting;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
@OwnedBy(HarnessTeam.CE)
public class AWSOrganizationHelperServiceImpl implements AWSOrganizationHelperService {
  private static final String ceAWSRegion = AWS_DEFAULT_REGION;

  @Override
  public List<CECloudAccount> getAWSAccounts(String accountId, String connectorId, CEAwsConnectorDTO ceAwsConnectorDTO,
      String awsAccessKey, String awsSecretKey) {
    List<CECloudAccount> ceCloudAccounts = new ArrayList<>();
    CrossAccountAccessDTO crossAccountAccess = ceAwsConnectorDTO.getCrossAccountAccess();
    List<Account> accountList = listAwsAccounts(crossAccountAccess, awsAccessKey, awsSecretKey);
    String masterAwsAccountId = getMasterAccountIdFromArn(crossAccountAccess.getCrossAccountRoleArn());
    String externalId = crossAccountAccess.getExternalId();
    accountList.forEach(account -> {
      String awsAccountId = getAccountIdFromArn(account.getArn());
      if (!awsAccountId.equals(masterAwsAccountId)) {
        CECloudAccount cloudAccount = CECloudAccount.builder()
                                          .accountId(accountId)
                                          .accountArn(account.getArn())
                                          .accountStatus(CECloudAccount.AccountStatus.NOT_VERIFIED)
                                          .accountName(account.getName())
                                          .infraAccountId(awsAccountId)
                                          .infraMasterAccountId(ceAwsConnectorDTO.getAwsAccountId())
                                          .masterAccountSettingId(connectorId)
                                          .build();
        ceCloudAccounts.add(cloudAccount);
      }
    });
    log.info("CE cloud account {}", ceCloudAccounts);
    return ceCloudAccounts;
  }

  public List<Account> listAwsAccounts(
      CrossAccountAccessDTO crossAccountAccess, String awsAccessKey, String awsSecretKey) {
    AWSOrganizationsClient awsOrganizationsClient =
        getAWSOrganizationsClient(crossAccountAccess, awsAccessKey, awsSecretKey);
    return listAwsAccounts(awsOrganizationsClient);
  }

  public static String getAccountIdFromArn(String arn) {
    return StringUtils.substringAfterLast(arn, "/");
  }

  public static String getMasterAccountIdFromArn(String arn) {
    return StringUtils.substringBefore(StringUtils.substringAfterLast(arn, "iam::"), ":");
  }

  @VisibleForTesting
  AWSOrganizationsClient getAWSOrganizationsClient(
      CrossAccountAccessDTO crossAccountAccess, String awsAccessKey, String awsSecretKey) {
    AWSSecurityTokenService awsSecurityTokenService = constructAWSSecurityTokenService(awsAccessKey, awsSecretKey);
    AWSOrganizationsClientBuilder builder = AWSOrganizationsClientBuilder.standard().withRegion(ceAWSRegion);
    AWSCredentialsProvider credentialsProvider =
        new STSAssumeRoleSessionCredentialsProvider
            .Builder(crossAccountAccess.getCrossAccountRoleArn(), UUID.randomUUID().toString())
            .withExternalId(crossAccountAccess.getExternalId())
            .withStsClient(awsSecurityTokenService)
            .build();
    builder.withCredentials(credentialsProvider);
    return (AWSOrganizationsClient) builder.build();
  }

  public AWSSecurityTokenService constructAWSSecurityTokenService(String awsAccessKey, String awsSecretKey) {
    AWSCredentialsProvider awsCredentialsProvider =
        new AWSStaticCredentialsProvider(new BasicAWSCredentials(awsAccessKey, awsSecretKey));
    return AWSSecurityTokenServiceClientBuilder.standard()
        .withRegion(ceAWSRegion)
        .withCredentials(awsCredentialsProvider)
        .build();
  }

  private List<Account> listAwsAccounts(AWSOrganizationsClient awsOrganizationsClient) {
    List<Account> accountList = new ArrayList<>();
    String nextToken = null;
    ListAccountsRequest listAccountsRequest = new ListAccountsRequest();
    do {
      listAccountsRequest.withNextToken(nextToken);
      ListAccountsResult listAccountsResult = awsOrganizationsClient.listAccounts(listAccountsRequest);
      accountList.addAll(listAccountsResult.getAccounts());
      nextToken = listAccountsResult.getNextToken();
    } while (nextToken != null);
    return accountList;
  }
}
