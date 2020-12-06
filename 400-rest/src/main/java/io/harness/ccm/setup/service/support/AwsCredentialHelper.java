package io.harness.ccm.setup.service.support;

import io.harness.ccm.setup.config.CESetUpConfig;

import software.wings.app.MainConfiguration;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.google.inject.Inject;

public class AwsCredentialHelper {
  @Inject private MainConfiguration configuration;
  private static final String ceAWSRegion = "us-east-1";

  public AWSSecurityTokenService constructAWSSecurityTokenService() {
    CESetUpConfig ceSetUpConfig = configuration.getCeSetUpConfig();
    AWSCredentialsProvider awsCredentialsProvider = new AWSStaticCredentialsProvider(
        new BasicAWSCredentials(ceSetUpConfig.getAwsAccessKey(), ceSetUpConfig.getAwsSecretKey()));
    return AWSSecurityTokenServiceClientBuilder.standard()
        .withRegion(ceAWSRegion)
        .withCredentials(awsCredentialsProvider)
        .build();
  }

  public AWSCredentialsProvider constructBasicAwsCredentials() {
    CESetUpConfig ceSetUpConfig = configuration.getCeSetUpConfig();
    return new AWSStaticCredentialsProvider(
        new BasicAWSCredentials(ceSetUpConfig.getAwsAccessKey(), ceSetUpConfig.getAwsSecretKey()));
  }

  public String getAWSS3Bucket() {
    return configuration.getCeSetUpConfig().getAwsS3BucketName();
  }
}
