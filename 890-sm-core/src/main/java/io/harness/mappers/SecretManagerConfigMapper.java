package io.harness.mappers;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SecretManagerConfig;
import io.harness.ng.core.mapper.TagMapper;
import io.harness.secretmanagerclient.NGSecretManagerMetadata;
import io.harness.secretmanagerclient.dto.GcpKmsConfigDTO;
import io.harness.secretmanagerclient.dto.GcpKmsConfigUpdateDTO;
import io.harness.secretmanagerclient.dto.LocalConfigDTO;
import io.harness.secretmanagerclient.dto.SecretManagerConfigDTO;
import io.harness.secretmanagerclient.dto.SecretManagerConfigUpdateDTO;
import io.harness.secretmanagerclient.dto.VaultConfigDTO;
import io.harness.secretmanagerclient.dto.VaultConfigUpdateDTO;

import software.wings.beans.GcpKmsConfig;
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
      case LOCAL:
        return LocalConfigMapper.fromDTO((LocalConfigDTO) dto);
      default:
        throw new UnsupportedOperationException("Secret Manager not supported");
    }
  }

  public static SecretManagerConfig applyUpdate(
      SecretManagerConfig secretManagerConfig, SecretManagerConfigUpdateDTO dto) {
    switch (dto.getEncryptionType()) {
      case VAULT:
        return VaultConfigMapper.applyUpdate((VaultConfig) secretManagerConfig, (VaultConfigUpdateDTO) dto);
      case GCP_KMS:
        return GcpKmsConfigMapper.applyUpdate((GcpKmsConfig) secretManagerConfig, (GcpKmsConfigUpdateDTO) dto);
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
    }
  }
}
