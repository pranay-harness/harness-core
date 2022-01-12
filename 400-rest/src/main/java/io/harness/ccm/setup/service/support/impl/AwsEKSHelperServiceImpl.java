/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ccm.setup.service.support.impl;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.setup.service.support.AwsCredentialHelper;
import io.harness.ccm.setup.service.support.intfc.AwsEKSHelperService;

import software.wings.beans.AwsCrossAccountAttributes;
import software.wings.service.impl.aws.client.CloseableAmazonWebServiceClient;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.STSAssumeRoleSessionCredentialsProvider;
import com.amazonaws.services.eks.AmazonEKSClient;
import com.amazonaws.services.eks.AmazonEKSClientBuilder;
import com.amazonaws.services.eks.model.ListClustersRequest;
import com.amazonaws.services.eks.model.ListClustersResult;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.model.AWSSecurityTokenServiceException;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(CE)
public class AwsEKSHelperServiceImpl implements AwsEKSHelperService {
  @Inject private AwsCredentialHelper awsCredentialHelper;
  private static final String exceptionMessage = "Error while calling cluster {} {} ";

  @Override
  public List<String> listEKSClusters(String region, AwsCrossAccountAttributes awsCrossAccountAttributes) {
    try (CloseableAmazonWebServiceClient<AmazonEKSClient> closeableAmazonEKSClient =
             new CloseableAmazonWebServiceClient(getAmazonEKSClient(region, awsCrossAccountAttributes))) {
      return listEKSClusters(closeableAmazonEKSClient.getClient());
    } catch (Exception ex) {
      log.info(exceptionMessage, region, ex);
      return Collections.emptyList();
    }
  }

  @Override
  public boolean verifyAccess(String region, AwsCrossAccountAttributes awsCrossAccountAttributes) {
    try (CloseableAmazonWebServiceClient<AmazonEKSClient> closeableAmazonEKSClient =
             new CloseableAmazonWebServiceClient(getAmazonEKSClient(region, awsCrossAccountAttributes))) {
      listEKSClusters(closeableAmazonEKSClient.getClient());
    } catch (AWSSecurityTokenServiceException stex) {
      log.info(exceptionMessage, region, stex);
      return false;
    } catch (Exception ex) {
      log.info(exceptionMessage, region, ex);
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
