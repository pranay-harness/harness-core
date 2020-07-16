package io.harness.batch.processing.cloudevents.aws.ecs.service.support.impl;

import com.google.common.annotations.VisibleForTesting;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.STSAssumeRoleSessionCredentialsProvider;
import com.amazonaws.services.ecs.AmazonECSClient;
import com.amazonaws.services.ecs.AmazonECSClientBuilder;
import com.amazonaws.services.ecs.model.ListClustersRequest;
import com.amazonaws.services.ecs.model.ListClustersResult;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import io.harness.batch.processing.cloudevents.aws.ecs.service.support.AwsCredentialHelper;
import io.harness.batch.processing.cloudevents.aws.ecs.service.support.intfc.AwsECSHelperService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.wings.beans.AwsCrossAccountAttributes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class AwsECSHelperServiceImpl implements AwsECSHelperService {
  @Autowired private AwsCredentialHelper awsCredentialHelper;
  private static final String exceptionMessage = "Error while calling cluster {} {} ";

  @Override
  public List<String> listECSClusters(String region, AwsCrossAccountAttributes awsCrossAccountAttributes) {
    try {
      AmazonECSClient amazonECSClient = getAmazonECSClient(region, awsCrossAccountAttributes);
      return listECSClusters(amazonECSClient);
    } catch (Exception ex) {
      logger.info(exceptionMessage, region, ex);
      return Collections.emptyList();
    }
  }

  @VisibleForTesting
  AmazonECSClient getAmazonECSClient(String region, AwsCrossAccountAttributes awsCrossAccountAttributes) {
    AWSSecurityTokenService awsSecurityTokenService = awsCredentialHelper.constructAWSSecurityTokenService();
    AmazonECSClientBuilder builder = AmazonECSClientBuilder.standard().withRegion(region);
    AWSCredentialsProvider credentialsProvider =
        new STSAssumeRoleSessionCredentialsProvider
            .Builder(awsCrossAccountAttributes.getCrossAccountRoleArn(), UUID.randomUUID().toString())
            .withExternalId(awsCrossAccountAttributes.getExternalId())
            .withStsClient(awsSecurityTokenService)
            .build();
    builder.withCredentials(credentialsProvider);
    return (AmazonECSClient) builder.build();
  }

  private List<String> listECSClusters(AmazonECSClient amazonECSClient) {
    List<String> clusterList = new ArrayList<>();
    String nextToken = null;
    ListClustersRequest listClustersRequest = new ListClustersRequest();
    do {
      listClustersRequest.withNextToken(nextToken);
      ListClustersResult listClustersResult = amazonECSClient.listClusters(listClustersRequest);
      clusterList.addAll(listClustersResult.getClusterArns());
      nextToken = listClustersResult.getNextToken();
    } while (nextToken != null);
    return clusterList;
  }
}
