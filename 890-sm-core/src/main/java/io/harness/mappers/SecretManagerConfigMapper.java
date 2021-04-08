package io.harness.mappers;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SecretManagerConfig;
import io.harness.ng.core.mapper.TagMapper;
import io.harness.secretmanagerclient.NGSecretManagerMetadata;
import io.harness.secretmanagerclient.dto.*;

import io.harness.secretmanagerclient.dto.awskms.AwsKmsConfigDTO;
import io.harness.secretmanagerclient.dto.awskms.AwsKmsConfigUpdateDTO;
import software.wings.beans.GcpKmsConfig;
import software.wings.beans.KmsConfig;
import software.wings.beans.VaultConfig;

import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(PL)
public class SecretManagerConfigMapper {
  public static SecretManagerConfig fromDTO(SecretManagerConfigDTO dto) {
    switch (dto.getEncryptionType()) {
      case VAULT:
        return VaultConfigMapper.fromDTO((VaultConfigDTO) dto);
      case GCP_KMS:
        return GcpKmsConfigMapper.fromDTO((GcpKmsConfigDTO) dto);
      case KMS:
        return AwsKmsConfigMapper.fromDTO((AwsKmsConfigDTO) dto);
      case LOCAL:
        return LocalConfigMapper.fromDTO((LocalConfigDTO) dto);
      default:
        throw new UnsupportedOperationException("Secret Manager not supported");
    }
  }

  public static SecretManagerConfig applyUpdate(SecretManagerConfig secretManagerConfig,
      SecretManagerConfigUpdateDTO dto, boolean secretsPresentInSecretManager) {
    switch (dto.getEncryptionType()) {
      case VAULT:
        return VaultConfigMapper.applyUpdate(
            (VaultConfig) secretManagerConfig, (VaultConfigUpdateDTO) dto, secretsPresentInSecretManager);
      case GCP_KMS:
        return GcpKmsConfigMapper.applyUpdate((GcpKmsConfig) secretManagerConfig, (GcpKmsConfigUpdateDTO) dto);
      case KMS:
        return AwsKmsConfigMapper.applyUpdate((KmsConfig) secretManagerConfig, (AwsKmsConfigUpdateDTO) dto);
      default:
        throw new UnsupportedOperationException("Secret Manager not supported");
    }
  }

  public static NGSecretManagerMetadata ngMetaDataFromDto(SecretManagerConfigDTO dto) {
    return NGSecretManagerMetadata.builder()
        .identifier(dto.getIdentifier())
        .projectIdentifier(dto.getProjectIdentifier())
        .orgIdentifier(dto.getOrgIdentifier())
        .accountIdentifier(dto.getAccountIdentifier())
        .description(dto.getDescription())
        .tags(TagMapper.convertToList(dto.getTags()))
        .harnessManaged(dto.isHarnessManaged())
        .build();
  }

  public static void updateNGSecretManagerMetadata(
      NGSecretManagerMetadata ngMetadata, SecretManagerConfigDTO secretManagerConfigDTO) {
    if (ngMetadata != null) {
      secretManagerConfigDTO.setAccountIdentifier(ngMetadata.getAccountIdentifier());
      secretManagerConfigDTO.setOrgIdentifier(ngMetadata.getOrgIdentifier());
      secretManagerConfigDTO.setProjectIdentifier(ngMetadata.getProjectIdentifier());
      secretManagerConfigDTO.setIdentifier(ngMetadata.getIdentifier());
      secretManagerConfigDTO.setTags(TagMapper.convertToMap(ngMetadata.getTags()));
      secretManagerConfigDTO.setDescription(ngMetadata.getDescription());
      secretManagerConfigDTO.setHarnessManaged(secretManagerConfigDTO.isHarnessManaged());
    }
  }
}
