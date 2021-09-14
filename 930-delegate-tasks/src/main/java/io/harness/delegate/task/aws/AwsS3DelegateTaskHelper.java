/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.task.aws;

import static io.harness.delegate.beans.connector.awsconnector.AwsCredentialType.INHERIT_FROM_DELEGATE;
import static io.harness.delegate.beans.connector.awsconnector.AwsCredentialType.IRSA;
import static io.harness.delegate.beans.connector.awsconnector.AwsCredentialType.MANUAL_CREDENTIALS;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;
import static io.harness.utils.FieldWithPlainTextOrSecretValueHelper.getSecretAsStringFromPlainTextOrSecretRef;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.aws.beans.AwsInternalConfig;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.connector.awsconnector.AwsConnectorDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsCredentialDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsManualConfigSpecDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsS3BucketResponse;
import io.harness.delegate.beans.connector.awsconnector.AwsTaskParams;
import io.harness.exception.InvalidArgumentsException;
import io.harness.security.encryption.SecretDecryptionService;

import software.wings.service.impl.AwsApiHelperService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@Slf4j
@Singleton
@OwnedBy(HarnessTeam.CDP)
public class AwsS3DelegateTaskHelper {
  private final SecretDecryptionService secretDecryptionService;
  private final AwsApiHelperService awsApiHelperService;

  public DelegateResponseData getS3Buckets(AwsTaskParams awsTaskParams) {
    decryptRequestDTOs(awsTaskParams);

    AwsInternalConfig awsInternalConfig = getAwsInternalConfig(awsTaskParams);
    List<String> buckets = awsApiHelperService.listS3Buckets(awsInternalConfig, awsTaskParams.getRegion());
    return AwsS3BucketResponse.builder()
        .commandExecutionStatus(SUCCESS)
        .buckets(buckets != null ? buckets.stream().collect(Collectors.toMap(s -> s, s -> s)) : Collections.emptyMap())
        .build();
  }

  private AwsInternalConfig getAwsInternalConfig(AwsTaskParams awsTaskParams) {
    AwsInternalConfig awsInternalConfig = AwsInternalConfig.builder().build();
    if (awsTaskParams.getAwsConnector() != null) {
      AwsCredentialDTO credential = awsTaskParams.getAwsConnector().getCredential();
      if (MANUAL_CREDENTIALS == credential.getAwsCredentialType()) {
        AwsManualConfigSpecDTO awsManualConfigSpecDTO = (AwsManualConfigSpecDTO) credential.getConfig();
        String accessKey = getSecretAsStringFromPlainTextOrSecretRef(
            awsManualConfigSpecDTO.getAccessKey(), awsManualConfigSpecDTO.getAccessKeyRef());
        if (accessKey == null) {
          throw new InvalidArgumentsException(Pair.of("accessKey", "Missing or empty"));
        }

        awsInternalConfig = AwsInternalConfig.builder()
                                .accessKey(accessKey.toCharArray())
                                .secretKey(awsManualConfigSpecDTO.getSecretKeyRef().getDecryptedValue())
                                .build();
      } else if (INHERIT_FROM_DELEGATE == credential.getAwsCredentialType()) {
        awsInternalConfig.setUseEc2IamCredentials(true);
      } else if (IRSA == credential.getAwsCredentialType()) {
        awsInternalConfig.setUseIRSA(true);
      }
    }
    return awsInternalConfig;
  }

  private void decryptRequestDTOs(AwsTaskParams awsTaskParams) {
    AwsConnectorDTO awsConnectorDTO = awsTaskParams.getAwsConnector();
    if (awsConnectorDTO.getCredential() != null && awsConnectorDTO.getCredential().getConfig() != null) {
      secretDecryptionService.decrypt(
          (AwsManualConfigSpecDTO) awsConnectorDTO.getCredential().getConfig(), awsTaskParams.getEncryptionDetails());
    }
  }
}
