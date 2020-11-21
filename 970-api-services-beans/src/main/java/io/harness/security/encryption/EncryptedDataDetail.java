package io.harness.security.encryption;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.security.encryption.EncryptionType.CUSTOM;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EncryptedDataDetail {
  private EncryptedRecordData encryptedData;
  private EncryptionConfig encryptionConfig;
  private String fieldName;

  public SecretUniqueIdentifier getIdentifier() {
    if (encryptionConfig.getEncryptionType() == CUSTOM) {
      return ParameterizedSecretUniqueIdentifier.builder()
          .parameters(encryptedData.getParameters())
          .kmsId(encryptionConfig.getUuid())
          .build();
    }

    if (isNotEmpty(encryptedData.getPath())) {
      return ReferencedSecretUniqueIdentifier.builder()
          .path(encryptedData.getPath())
          .kmsId(encryptionConfig.getUuid())
          .build();
    }

    return InlineSecretUniqueIdentifier.builder()
        .encryptionKey(encryptedData.getEncryptionKey())
        .kmsId(encryptionConfig.getUuid())
        .build();
  }
}
