/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing.cloudevents.aws.ecs.service.support;

import io.harness.batch.processing.config.BatchMainConfig;

import software.wings.security.authentication.AwsS3SyncConfig;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AwsCredentialHelper {
  @Autowired private BatchMainConfig batchMainConfig;
  private static final String ceAWSRegion = "us-east-1";

  public AWSSecurityTokenService constructAWSSecurityTokenService() {
    AwsS3SyncConfig awsS3SyncConfig = batchMainConfig.getAwsS3SyncConfig();
    AWSCredentialsProvider awsCredentialsProvider = new AWSStaticCredentialsProvider(
        new BasicAWSCredentials(awsS3SyncConfig.getAwsAccessKey(), awsS3SyncConfig.getAwsSecretKey()));
    return AWSSecurityTokenServiceClientBuilder.standard()
        .withRegion(ceAWSRegion)
        .withCredentials(awsCredentialsProvider)
        .build();
  }
}
