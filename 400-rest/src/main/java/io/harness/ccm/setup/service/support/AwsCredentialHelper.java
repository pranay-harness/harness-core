/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ccm.setup.service.support;

import static io.harness.annotations.dev.HarnessTeam.CE;

import static software.wings.service.impl.aws.model.AwsConstants.AWS_DEFAULT_REGION;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.setup.config.CESetUpConfig;

import software.wings.app.MainConfiguration;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.google.inject.Inject;

@OwnedBy(CE)
public class AwsCredentialHelper {
  @Inject private MainConfiguration configuration;
  private static final String ceAWSRegion = AWS_DEFAULT_REGION;

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
