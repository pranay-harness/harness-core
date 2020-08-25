package io.harness.ng.core.utils;

import static io.harness.delegate.beans.connector.ConnectorType.GCP_KMS;
import static io.harness.delegate.beans.connector.ConnectorType.LOCAL;

import io.harness.connector.apis.dto.ConnectorRequestDTO;
import io.harness.eraro.ErrorCode;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.secretmanagerclient.dto.GcpKmsConfigDTO;
import io.harness.secretmanagerclient.dto.GcpKmsConnectorDTO;
import io.harness.secretmanagerclient.dto.LocalConnectorDTO;
import io.harness.secretmanagerclient.dto.SecretManagerConfigDTO;
import io.harness.security.encryption.EncryptionType;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.tuple.Pair;
import software.wings.service.impl.security.SecretManagementException;

import java.util.List;

@UtilityClass
public class NGUtils {
  public static void verifyValuesNotChanged(List<Pair<?, ?>> valuesList) {
    for (Pair<?, ?> pair : valuesList) {
      if (!pair.getKey().equals(pair.getValue())) {
        throw new InvalidRequestException(
            "Value mismatch, previous: " + pair.getKey() + " current: " + pair.getValue());
      }
    }
  }

  public static ConnectorRequestDTO getConnectorRequestDTO(SecretManagerConfigDTO secretManagerConfigDTO) {
    switch (secretManagerConfigDTO.getEncryptionType()) {
      case GCP_KMS:
        GcpKmsConfigDTO gcpKmsConfig = (GcpKmsConfigDTO) secretManagerConfigDTO;
        GcpKmsConnectorDTO gcpKmsConnectorDTO = GcpKmsConnectorDTO.builder()
                                                    .region(gcpKmsConfig.getRegion())
                                                    .keyRing(gcpKmsConfig.getKeyRing())
                                                    .keyName(gcpKmsConfig.getKeyName())
                                                    .projectId(gcpKmsConfig.getProjectId())
                                                    .credentials(gcpKmsConfig.getCredentials())
                                                    .isDefault(secretManagerConfigDTO.isDefault())
                                                    .build();
        return ConnectorRequestDTO.builder()
            .connectorType(GCP_KMS)
            .identifier(secretManagerConfigDTO.getIdentifier())
            .name(secretManagerConfigDTO.getName())
            .orgIdentifier(secretManagerConfigDTO.getOrgIdentifier())
            .projectIdentifier(secretManagerConfigDTO.getProjectIdentifier())
            .description(secretManagerConfigDTO.getDescription())
            .connectorConfig(gcpKmsConnectorDTO)
            .build();
      case LOCAL:
        LocalConnectorDTO localConnectorDTO =
            LocalConnectorDTO.builder().isDefault(secretManagerConfigDTO.isDefault()).build();
        return ConnectorRequestDTO.builder()
            .connectorType(LOCAL)
            .identifier(secretManagerConfigDTO.getIdentifier())
            .name(secretManagerConfigDTO.getName())
            .orgIdentifier(secretManagerConfigDTO.getOrgIdentifier())
            .projectIdentifier(secretManagerConfigDTO.getProjectIdentifier())
            .description(secretManagerConfigDTO.getDescription())
            .connectorConfig(localConnectorDTO)
            .build();
      default:
        throw new SecretManagementException(
            ErrorCode.SECRET_MANAGEMENT_ERROR, "Unsupported Secret Manager", WingsException.USER);
    }
  }

  public static String getDefaultHarnessSecretManagerName(EncryptionType encryptionType) {
    switch (encryptionType) {
      case GCP_KMS:
        return "Harness Secrets Manager - Google KMS";
      case LOCAL:
        return "Harness Vault";
      default:
        throw new SecretManagementException(
            ErrorCode.SECRET_MANAGEMENT_ERROR, "Unsupported Harness Secret Manager", WingsException.USER);
    }
  }
}
