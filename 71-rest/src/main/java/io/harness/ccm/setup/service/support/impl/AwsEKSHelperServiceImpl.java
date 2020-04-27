package io.harness.ccm.setup.service.support.impl;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.STSAssumeRoleSessionCredentialsProvider;
import com.amazonaws.services.eks.AmazonEKSClient;
import com.amazonaws.services.eks.AmazonEKSClientBuilder;
import com.amazonaws.services.eks.model.ListClustersRequest;
import com.amazonaws.services.eks.model.ListClustersResult;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.model.AWSSecurityTokenServiceException;
import io.harness.ccm.setup.service.support.AwsCredentialHelper;
import io.harness.ccm.setup.service.support.intfc.AwsEKSHelperService;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.AwsCrossAccountAttributes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
public class AwsEKSHelperServiceImpl implements AwsEKSHelperService {
  @Inject private AwsCredentialHelper awsCredentialHelper;
  private static final String exceptionMessage = "Error while calling cluster {} {} ";

  @Override
  public List<String> listEKSClusters(String region, AwsCrossAccountAttributes awsCrossAccountAttributes) {
    try {
      AmazonEKSClient amazonEKSClient = getAmazonEKSClient(region, awsCrossAccountAttributes);
      return listEKSClusters(amazonEKSClient);
    } catch (Exception ex) {
      logger.info(exceptionMessage, region, ex);
      return Collections.emptyList();
    }
  }

  @Override
  public boolean verifyAccess(String region, AwsCrossAccountAttributes awsCrossAccountAttributes) {
    try {
      AmazonEKSClient amazonEKSClient = getAmazonEKSClient(region, awsCrossAccountAttributes);
      listEKSClusters(amazonEKSClient);
    } catch (AWSSecurityTokenServiceException stex) {
      logger.info(exceptionMessage, region, stex);
      return false;
    } catch (Exception ex) {
      logger.info(exceptionMessage, region, ex);
    }
    return true;
  }

  @VisibleForTesting
  AmazonEKSClient getAmazonEKSClient(String region, AwsCrossAccountAttributes awsCrossAccountAttributes) {
    AWSSecurityTokenService awsSecurityTokenService = awsCredentialHelper.constructAWSSecurityTokenService();
    AmazonEKSClientBuilder builder = AmazonEKSClientBuilder.standard().withRegion(region);
    AWSCredentialsProvider credentialsProvider =
        new STSAssumeRoleSessionCredentialsProvider
            .Builder(awsCrossAccountAttributes.getCrossAccountRoleArn(), UUID.randomUUID().toString())
            .withExternalId(awsCrossAccountAttributes.getExternalId())
            .withStsClient(awsSecurityTokenService)
            .build();
    builder.withCredentials(credentialsProvider);
    return (AmazonEKSClient) builder.build();
  }

  private List<String> listEKSClusters(AmazonEKSClient amazonEKSClient) {
    List<String> clusterList = new ArrayList<>();
    String nextToken = null;
    ListClustersRequest listClustersRequest = new ListClustersRequest();
    do {
      listClustersRequest.withNextToken(nextToken);
      ListClustersResult listClustersResult = amazonEKSClient.listClusters(listClustersRequest);
      clusterList.addAll(listClustersResult.getClusters());
      nextToken = listClustersResult.getNextToken();
    } while (nextToken != null);
    return clusterList;
  }
}
