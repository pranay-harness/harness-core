package io.harness.ccm.setup.service.support.impl;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.STSAssumeRoleSessionCredentialsProvider;
import com.amazonaws.services.organizations.AWSOrganizationsClient;
import com.amazonaws.services.organizations.AWSOrganizationsClientBuilder;
import com.amazonaws.services.organizations.model.Account;
import com.amazonaws.services.organizations.model.ListAccountsRequest;
import com.amazonaws.services.organizations.model.ListAccountsResult;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import io.harness.ccm.setup.service.support.AwsCredentialHelper;
import io.harness.ccm.setup.service.support.intfc.AWSOrganizationHelperService;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.AwsCrossAccountAttributes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
public class AWSOrganizationHelperServiceImpl implements AWSOrganizationHelperService {
  @Inject private AwsCredentialHelper awsCredentialHelper;

  private static final String ceAWSRegion = "us-east-1";

  @Override
  public List<Account> listAwsAccounts(AwsCrossAccountAttributes awsCrossAccountAttributes) {
    try {
      AWSOrganizationsClient awsOrganizationsClient = getAWSOrganizationsClient(awsCrossAccountAttributes);
      return listAwsAccounts(awsOrganizationsClient);
    } catch (Exception ex) {
      logger.error("Error while getting accounts", ex);
    }
    return Collections.emptyList();
  }

  @VisibleForTesting
  AWSOrganizationsClient getAWSOrganizationsClient(AwsCrossAccountAttributes awsCrossAccountAttributes) {
    AWSSecurityTokenService awsSecurityTokenService = awsCredentialHelper.constructAWSSecurityTokenService();
    AWSOrganizationsClientBuilder builder = AWSOrganizationsClientBuilder.standard().withRegion(ceAWSRegion);
    AWSCredentialsProvider credentialsProvider =
        new STSAssumeRoleSessionCredentialsProvider
            .Builder(awsCrossAccountAttributes.getCrossAccountRoleArn(), UUID.randomUUID().toString())
            .withExternalId(awsCrossAccountAttributes.getExternalId())
            .withStsClient(awsSecurityTokenService)
            .build();
    builder.withCredentials(credentialsProvider);
    return (AWSOrganizationsClient) builder.build();
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
