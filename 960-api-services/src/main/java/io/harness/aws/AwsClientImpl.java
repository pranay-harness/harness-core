package io.harness.aws;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import static org.apache.commons.lang3.StringUtils.defaultString;

import io.harness.exception.InvalidRequestException;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.EC2ContainerCredentialsProviderWrapper;
import com.amazonaws.auth.STSAssumeRoleSessionCredentialsProvider;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.AmazonEC2Exception;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class AwsClientImpl implements AwsClient {
  @Inject protected AwsCallTracker tracker;

  @Override
  public void validateAwsAccountCredential(AwsConfig awsConfig) {
    try {
      tracker.trackEC2Call("Get Ec2 client");
      getAmazonEc2Client(Regions.US_EAST_1.getName(), awsConfig).describeRegions();
    } catch (AmazonEC2Exception amazonEC2Exception) {
      if (amazonEC2Exception.getStatusCode() == 401) {
        if (!awsConfig.isEc2IamCredentials()) {
          if (isEmpty(awsConfig.getAwsAccessKeyCredential().getAccessKey())) {
            throw new InvalidRequestException("Access Key should not be empty");
          } else if (isEmpty(awsConfig.getAwsAccessKeyCredential().getSecretKey())) {
            throw new InvalidRequestException("Secret Key should not be empty");
          }
        }
      }
      throw amazonEC2Exception;
    }
  }

  @VisibleForTesting
  AmazonEC2Client getAmazonEc2Client(String region, AwsConfig awsConfig) {
    AmazonEC2ClientBuilder builder = AmazonEC2ClientBuilder.standard().withRegion(region);
    attachCredentials(builder, awsConfig);
    return (AmazonEC2Client) builder.build();
  }

  protected void attachCredentials(AwsClientBuilder builder, AwsConfig awsConfig) {
    AWSCredentialsProvider credentialsProvider;
    if (awsConfig.isEc2IamCredentials()) {
      log.info("Instantiating EC2ContainerCredentialsProviderWrapper");
      credentialsProvider = new EC2ContainerCredentialsProviderWrapper();
    } else {
      credentialsProvider = new AWSStaticCredentialsProvider(
          new BasicAWSCredentials(defaultString(awsConfig.getAwsAccessKeyCredential().getAccessKey(), ""),
              awsConfig.getAwsAccessKeyCredential().getSecretKey() != null
                  ? new String(awsConfig.getAwsAccessKeyCredential().getSecretKey())
                  : ""));
    }
    if (awsConfig.getCrossAccountAccess() != null) {
      // For the security token service we default to us-east-1.
      AWSSecurityTokenService securityTokenService = AWSSecurityTokenServiceClientBuilder.standard()
                                                         .withRegion(Regions.US_EAST_1.getName())
                                                         .withCredentials(credentialsProvider)
                                                         .build();
      CrossAccountAccess crossAccountAttributes = awsConfig.getCrossAccountAccess();
      credentialsProvider = new STSAssumeRoleSessionCredentialsProvider
                                .Builder(crossAccountAttributes.getCrossAccountRoleArn(), UUID.randomUUID().toString())
                                .withStsClient(securityTokenService)
                                .withExternalId(crossAccountAttributes.getExternalId())
                                .build();
    }
    builder.withCredentials(credentialsProvider);
  }
}
