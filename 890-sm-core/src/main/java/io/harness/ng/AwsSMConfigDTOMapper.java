/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.ConnectorDTO;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.delegate.beans.connector.awssecretmanager.AwsSMCredentialSpecAssumeSTSDTO;
import io.harness.delegate.beans.connector.awssecretmanager.AwsSMCredentialSpecManualConfigDTO;
import io.harness.delegate.beans.connector.awssecretmanager.AwsSecretManagerCredentialDTO;
import io.harness.delegate.beans.connector.awssecretmanager.AwsSecretManagerCredentialType;
import io.harness.delegate.beans.connector.awssecretmanager.AwsSecretManagerDTO;
import io.harness.exception.InvalidRequestException;
import io.harness.secretmanagerclient.dto.awssecretmanager.AwsSMConfigDTO;
import io.harness.secretmanagerclient.dto.awssecretmanager.AwsSMConfigUpdateDTO;
import io.harness.secretmanagerclient.dto.awssecretmanager.AwsSMCredentialSpecConfig;
import io.harness.secretmanagerclient.dto.awssecretmanager.AwsSMManualCredentialConfig;
import io.harness.secretmanagerclient.dto.awssecretmanager.AwsSMStsCredentialConfig;
import io.harness.secretmanagerclient.dto.awssecretmanager.BaseAwsSMConfigDTO;
import io.harness.security.encryption.EncryptionType;

import lombok.experimental.UtilityClass;

@OwnedBy(PL)
@UtilityClass
public class AwsSMConfigDTOMapper {
  public static AwsSMConfigDTO getAwsSMConfigDTO(
      String accountIdentifier, ConnectorDTO connectorRequestDTO, AwsSecretManagerDTO awsSecretManagerDTO) {
    ConnectorInfoDTO connector = connectorRequestDTO.getConnectorInfo();
    return AwsSMConfigDTO.builder()
        .baseAwsSMConfigDTO(buildBaseProperties(awsSecretManagerDTO))
        .isDefault(false)
        .encryptionType(EncryptionType.AWS_SECRETS_MANAGER)
        .name(connector.getName())
        .accountIdentifier(accountIdentifier)
        .orgIdentifier(connector.getOrgIdentifier())
        .projectIdentifier(connector.getProjectIdentifier())
        .tags(connector.getTags())
        .identifier(connector.getIdentifier())
        .description(connector.getDescription())
        .harnessManaged(awsSecretManagerDTO.isHarnessManaged())
        .build();
  }

  public static AwsSMConfigUpdateDTO getAwsSMConfigUpdateDTO(
      ConnectorDTO connectorRequestDTO, AwsSecretManagerDTO awsSecretManagerDTO) {
    ConnectorInfoDTO connector = connectorRequestDTO.getConnectorInfo();
    return AwsSMConfigUpdateDTO.builder()
        .baseAwsSMConfigDTO(buildBaseProperties(awsSecretManagerDTO))
        .name(connector.getName())
        .isDefault(false)
        .encryptionType(EncryptionType.AWS_SECRETS_MANAGER)
        .tags(connector.getTags())
        .description(connector.getDescription())
        .build();
  }

  private static BaseAwsSMConfigDTO buildBaseProperties(AwsSecretManagerDTO awsSecretManagerDTO) {
    return BaseAwsSMConfigDTO.builder()
        .region(awsSecretManagerDTO.getRegion())
        .secretNamePrefix(awsSecretManagerDTO.getSecretNamePrefix())
        .credential(populateCredentials(awsSecretManagerDTO))
        .credentialType(awsSecretManagerDTO.getCredential().getCredentialType())
        .delegateSelectors(awsSecretManagerDTO.getDelegateSelectors())
        .build();
  }

  private static AwsSMCredentialSpecConfig populateCredentials(AwsSecretManagerDTO awsSecretManagerDTO) {
    AwsSecretManagerCredentialDTO credential = awsSecretManagerDTO.getCredential();
    AwsSecretManagerCredentialType credentialType = credential.getCredentialType();
    AwsSMCredentialSpecConfig awsSMCredentialSpecConfig;
    switch (credentialType) {
      case MANUAL_CONFIG:
        awsSMCredentialSpecConfig = buildManualConfig((AwsSMCredentialSpecManualConfigDTO) credential.getConfig());
        break;
      case ASSUME_IAM_ROLE:
        awsSMCredentialSpecConfig = null;
        break;
      case ASSUME_STS_ROLE:
        awsSMCredentialSpecConfig = buildStsConfig((AwsSMCredentialSpecAssumeSTSDTO) credential.getConfig());
        break;
      default:
        throw new InvalidRequestException("Invalid Credential type.");
    }
    return awsSMCredentialSpecConfig;
  }

  private static AwsSMManualCredentialConfig buildManualConfig(AwsSMCredentialSpecManualConfigDTO configDTO) {
    final String accessKey = configDTO.getAccessKey().getDecryptedValue() == null
        ? null
        : String.valueOf(configDTO.getAccessKey().getDecryptedValue());
    final String secretKey = configDTO.getSecretKey().getDecryptedValue() == null
        ? null
        : String.valueOf(configDTO.getSecretKey().getDecryptedValue());
    return AwsSMManualCredentialConfig.builder()
        .accessKey(String.valueOf(accessKey))
        .secretKey(String.valueOf(secretKey))
        .build();
  }

  private static AwsSMStsCredentialConfig buildStsConfig(AwsSMCredentialSpecAssumeSTSDTO configDTO) {
    return AwsSMStsCredentialConfig.builder()
        .externalId(configDTO.getExternalId())
        .roleArn(configDTO.getRoleArn())
        .assumeStsRoleDuration(configDTO.getAssumeStsRoleDuration())
        .build();
  }
}
