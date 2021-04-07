package io.harness.ng;

import io.harness.connector.ConnectorDTO;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.delegate.beans.connector.awskmsconnector.AwsKmsConnectorDTO;
import io.harness.secretmanagerclient.dto.AwsKmsConfigDTO;
import io.harness.secretmanagerclient.dto.AwsKmsConfigUpdateDTO;
import io.harness.secretmanagerclient.dto.BaseAwsKmsConfigDTO;
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
            //.accessKey(awsKmsConnectorDTO.getAccessKey())
            //.secretKey(awsKmsConnectorDTO.getSecretKey())
            .kmsArn(awsKmsConnectorDTO.getKmsArn())
            .region(awsKmsConnectorDTO.getRegion()).build();
  }

}
