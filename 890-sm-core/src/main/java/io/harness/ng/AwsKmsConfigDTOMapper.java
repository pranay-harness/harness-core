package io.harness.ng;

import io.harness.connector.ConnectorDTO;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.delegate.beans.connector.awskmsconnector.*;
import io.harness.exception.InvalidRequestException;
import io.harness.secretmanagerclient.dto.awskms.*;
import io.harness.security.encryption.EncryptionType;
import lombok.experimental.UtilityClass;

@UtilityClass
public class AwsKmsConfigDTOMapper {
  public static AwsKmsConfigDTO getAwsKmsConfigDTO(
      String accountIdentifier, ConnectorDTO connectorRequestDTO, AwsKmsConnectorDTO awsKmsConnectorDTO) {
    ConnectorInfoDTO connector = connectorRequestDTO.getConnectorInfo();
    return AwsKmsConfigDTO.builder()
        .baseAwsKmsConfigDTO(buildBaseProperties(awsKmsConnectorDTO))
        .isDefault(false)
        .encryptionType(EncryptionType.KMS)

        .name(connector.getName())
        .accountIdentifier(accountIdentifier)
        .orgIdentifier(connector.getOrgIdentifier())
        .projectIdentifier(connector.getProjectIdentifier())
        .tags(connector.getTags())
        .identifier(connector.getIdentifier())
        .description(connector.getDescription())
        .harnessManaged(awsKmsConnectorDTO.isHarnessManaged())
        .build();
  }

  public static AwsKmsConfigUpdateDTO getAwsKmsConfigUpdateDTO(
      ConnectorDTO connectorRequestDTO, AwsKmsConnectorDTO awsKmsConnectorDTO) {
    ConnectorInfoDTO connector = connectorRequestDTO.getConnectorInfo();
    return AwsKmsConfigUpdateDTO.builder()
        .baseAwsKmsConfigDTO(buildBaseProperties(awsKmsConnectorDTO))
        .isDefault(false)
        .encryptionType(EncryptionType.KMS)

        .tags(connector.getTags())
        .description(connector.getDescription())
        .build();
  }

  private static BaseAwsKmsConfigDTO buildBaseProperties(AwsKmsConnectorDTO awsKmsConnectorDTO) {
    return BaseAwsKmsConfigDTO.builder()
            .region(awsKmsConnectorDTO.getRegion())
            .kmsArn(awsKmsConnectorDTO.getKmsArn())
            .name(awsKmsConnectorDTO.getName())
            .credential(populateCredentials(awsKmsConnectorDTO))
            .credentialType(awsKmsConnectorDTO.getCredential().getCredentialType())
            .build();
  }

  private static AwsKmsCredentialSpecConfig populateCredentials(AwsKmsConnectorDTO awsKmsConnectorDTO) {
    AwsKmsConnectorCredentialDTO credential = awsKmsConnectorDTO.getCredential();
    AwsKmsCredentialType credentialType = credential.getCredentialType();
    AwsKmsCredentialSpecConfig awsKmsCredentialSpecConfig;
    switch (credentialType){
      case MANUAL_CONFIG:
        awsKmsCredentialSpecConfig = buildManualConfig((AwsKmsCredentialSpecManualConfigDTO) credential.getConfig());
        break;
      case ASSUME_IAM_ROLE:
        awsKmsCredentialSpecConfig = buildIamConfig((AwsKmsCredentialSpecAssumeIAMDTO) credential.getConfig());
        break;
      case ASSUME_STS_ROLE:
        awsKmsCredentialSpecConfig = buildStsConfig((AwsKmsCredentialSpecAssumeSTSDTO) credential.getConfig());
        break;
      default:
        throw new InvalidRequestException("Invalid Credential type.");
    }
    return awsKmsCredentialSpecConfig;
  }

  private static AwsKmsCredentialSpecConfig buildManualConfig(AwsKmsCredentialSpecManualConfigDTO configDTO) {
    return AwsKmsManualCredentialConfig.builder()
            .accessKey(configDTO.getAccessKey())
            .secretKey(configDTO.getSecretKey())
            .build();
  }

  private static AwsKmsCredentialSpecConfig buildIamConfig(AwsKmsCredentialSpecAssumeIAMDTO configDTO) {
    return AwsKmsIamCredentialConfig.builder()
            .delegateSelectors(configDTO.getDelegateSelectors())
            .build();
  }

  private static AwsKmsCredentialSpecConfig buildStsConfig(AwsKmsCredentialSpecAssumeSTSDTO configDTO) {
    return AwsKmsStsCredentialConfig.builder()
            .delegateSelectors(configDTO.getDelegateSelectors())
            .externalName(configDTO.getExternalName())
            .roleArn(configDTO.getRoleArn())
            .build();
  }
}
