package io.harness.ng.core.api.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.api.NGEncryptedDataService;
import io.harness.ng.core.entities.NGEncryptedData;
import io.harness.repositories.NGEncryptedDataRepository;
import io.harness.security.encryption.EncryptedRecord;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PL)
@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class NGEncryptedDataServiceImpl implements NGEncryptedDataService {
  private final NGEncryptedDataRepository encryptedDataRepository;

  @Override
  public NGEncryptedData create(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      String identifier, EncryptedRecord encryptedRecord) {
    NGEncryptedData ngEncryptedData = NGEncryptedData.builder()
                                          .accountIdentifier(accountIdentifier)
                                          .orgIdentifier(orgIdentifier)
                                          .projectIdentifier(projectIdentifier)
                                          .identifier(identifier)
                                          .name(encryptedRecord.getName())
                                          .secretManagerIdentifier(encryptedRecord.getKmsId())
                                          .parameters(encryptedRecord.getParameters())
                                          .path(encryptedRecord.getPath())
                                          .encryptionKey(encryptedRecord.getEncryptionKey())
                                          .encryptedValue(encryptedRecord.getEncryptedValue())
                                          .encryptionType(encryptedRecord.getEncryptionType())
                                          .backupEncryptionType(encryptedRecord.getBackupEncryptionType())
                                          .backupEncryptionKey(encryptedRecord.getBackupEncryptionKey())
                                          .backupEncryptedValue(encryptedRecord.getBackupEncryptedValue())
                                          .backupKmsId(encryptedRecord.getBackupKmsId())
                                          .base64Encoded(encryptedRecord.isBase64Encoded())
                                          .additionalMetadata(encryptedRecord.getAdditionalMetadata())
                                          .build();
    return encryptedDataRepository.save(ngEncryptedData);
  }

  @Override
  public NGEncryptedData get(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier) {
    Optional<NGEncryptedData> ngEncryptedData =
        encryptedDataRepository.findNGEncryptedDataByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndIdentifier(
            accountIdentifier, orgIdentifier, projectIdentifier, identifier);
    return ngEncryptedData.orElse(null);
  }

  @Override
  public boolean delete(String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier) {
    return encryptedDataRepository
        .deleteNGEncryptedDataByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndIdentifier(
            accountIdentifier, orgIdentifier, projectIdentifier, identifier);
  }
}
