package io.harness.ng.core.remote;

import io.harness.ng.core.dto.SecretManagerConfigDTO;
import lombok.experimental.UtilityClass;
import software.wings.beans.SecretManagerConfig;

@UtilityClass
public class SecretManagerConfigMapper {
  static SecretManagerConfigDTO writeDTO(SecretManagerConfig secretManagerConfig) {
    return SecretManagerConfigDTO.builder()
        .uuid(secretManagerConfig.getUuid())
        .encryptionType(secretManagerConfig.getEncryptionType())
        .isDefault(secretManagerConfig.isDefault())
        .accountId(secretManagerConfig.getAccountId())
        .numOfEncryptedValue(secretManagerConfig.getNumOfEncryptedValue())
        .encryptedBy(secretManagerConfig.getEncryptedBy())
        .templatizedFields(secretManagerConfig.getTemplatizedFields())
        .nextTokenRenewIteration(secretManagerConfig.getNextTokenRenewIteration())
        .build();
  }
}
