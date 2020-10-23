package io.harness.mappers;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.secretmanagerclient.NGSecretManagerMetadata;
import io.harness.secretmanagerclient.dto.GcpKmsConfigDTO;
import io.harness.secretmanagerclient.dto.GcpKmsConfigUpdateDTO;
import lombok.experimental.UtilityClass;
import software.wings.beans.GcpKmsConfig;

import java.util.Optional;

@UtilityClass
@OwnedBy(PL)
public class GcpKmsConfigMapper {
  public static GcpKmsConfig fromDTO(GcpKmsConfigDTO gcpKmsConfigDTO) {
    GcpKmsConfig gcpKmsConfig =
        new GcpKmsConfig(gcpKmsConfigDTO.getName(), gcpKmsConfigDTO.getProjectId(), gcpKmsConfigDTO.getRegion(),
            gcpKmsConfigDTO.getKeyRing(), gcpKmsConfigDTO.getKeyName(), gcpKmsConfigDTO.getCredentials());
    gcpKmsConfig.setNgMetadata(SecretManagerConfigMapper.ngMetaDataFromDto(gcpKmsConfigDTO));
    gcpKmsConfig.setAccountId(gcpKmsConfigDTO.getAccountIdentifier());
    gcpKmsConfig.setEncryptionType(gcpKmsConfigDTO.getEncryptionType());
    gcpKmsConfig.setDefault(gcpKmsConfigDTO.isDefault());
    return gcpKmsConfig;
  }

  public static GcpKmsConfig applyUpdate(GcpKmsConfig gcpKmsConfig, GcpKmsConfigUpdateDTO gcpKmsConfigDTO) {
    gcpKmsConfig.setProjectId(gcpKmsConfigDTO.getProjectId());
    gcpKmsConfig.setCredentials(gcpKmsConfigDTO.getCredentials());
    gcpKmsConfig.setKeyName(gcpKmsConfigDTO.getKeyName());
    gcpKmsConfig.setKeyRing(gcpKmsConfigDTO.getKeyRing());
    gcpKmsConfig.setDefault(gcpKmsConfigDTO.isDefault());
    gcpKmsConfig.setRegion(gcpKmsConfigDTO.getRegion());
    if (!Optional.ofNullable(gcpKmsConfig.getNgMetadata()).isPresent()) {
      gcpKmsConfig.setNgMetadata(NGSecretManagerMetadata.builder().build());
    }
    gcpKmsConfig.getNgMetadata().setTags(gcpKmsConfigDTO.getTags());
    gcpKmsConfig.getNgMetadata().setDescription(gcpKmsConfigDTO.getDescription());
    return gcpKmsConfig;
  }
}
