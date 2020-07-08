package io.harness.ng.core.remote;

import io.harness.ng.core.dto.EncryptedDataDTO;
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
        .type(encryptedData.getType())
        .parents(encryptedData.getParents())
        .accountId(encryptedData.getAccountId())
        .enabled(encryptedData.isEnabled())
        .kmsId(encryptedData.getKmsId())
        .encryptionType(encryptedData.getEncryptionType())
        .fileSize(encryptedData.getFileSize())
        .appIds(encryptedData.getAppIds())
        .serviceIds(encryptedData.getServiceIds())
        .envIds(encryptedData.getEnvIds())
        .backupEncryptedValue(encryptedData.getBackupEncryptedValue())
        .backupEncryptionKey(encryptedData.getBackupEncryptionKey())
        .backupKmsId(encryptedData.getBackupKmsId())
        .backupEncryptionType(encryptedData.getBackupEncryptionType())
        .serviceVariableIds(encryptedData.getServiceVariableIds())
        .searchTags(encryptedData.getSearchTags())
        .scopedToAccount(encryptedData.isScopedToAccount())
        .usageRestrictions(encryptedData.getUsageRestrictions())
        .nextMigrationIteration(encryptedData.getNextMigrationIteration())
        .nextAwsToGcpKmsMigrationIteration(encryptedData.getNextAwsToGcpKmsMigrationIteration())
        .base64Encoded(encryptedData.isBase64Encoded())
        .encryptedBy(encryptedData.getEncryptedBy())
        .setupUsage(encryptedData.getSetupUsage())
        .runTimeUsage(encryptedData.getRunTimeUsage())
        .changeLog(encryptedData.getChangeLog())
        .keywords(encryptedData.getKeywords())
        .uuid(encryptedData.getUuid())
        .appId(encryptedData.getAppId())
        .entityYamlPath(encryptedData.getEntityYamlPath())
        .build();
  }
}
