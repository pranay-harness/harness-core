package io.harness.ng.core.remote;

import io.harness.secretmanagerclient.dto.EncryptedDataDTO;
import lombok.experimental.UtilityClass;
import software.wings.security.encryption.EncryptedData;

@UtilityClass
public class EncryptedDataMapper {
  static EncryptedDataDTO writeDTO(EncryptedData encryptedData) {
    return EncryptedDataDTO.builder()
        .name(encryptedData.getName())
        .encryptionKey(encryptedData.getEncryptionKey())
        .encryptedValue(encryptedData.getEncryptedValue())
        .path(encryptedData.getPath())
        .parameters(encryptedData.getParameters())
        .accountId(encryptedData.getAccountId())
        .enabled(encryptedData.isEnabled())
        .kmsId(encryptedData.getKmsId())
        .encryptionType(encryptedData.getEncryptionType())
        .fileSize(encryptedData.getFileSize())
        .backupEncryptedValue(encryptedData.getBackupEncryptedValue())
        .backupEncryptionKey(encryptedData.getBackupEncryptionKey())
        .backupKmsId(encryptedData.getBackupKmsId())
        .backupEncryptionType(encryptedData.getBackupEncryptionType())
        .scopedToAccount(encryptedData.isScopedToAccount())
        .base64Encoded(encryptedData.isBase64Encoded())
        .uuid(encryptedData.getUuid())
        .entityYamlPath(encryptedData.getEntityYamlPath())
        .build();
  }
}
