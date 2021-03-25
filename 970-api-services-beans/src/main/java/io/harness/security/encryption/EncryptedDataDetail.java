package io.harness.security.encryption;

import static io.harness.data.structure.HasPredicate.hasSome;
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
    String kmsId = hasSome(encryptionConfig.getUuid()) ? encryptionConfig.getUuid() : encryptedData.getKmsId();

    if (encryptionConfig.getEncryptionType() == CUSTOM) {
      return ParameterizedSecretUniqueIdentifier.builder()
          .parameters(encryptedData.getParameters())
          .kmsId(kmsId)
          .build();
    }

    if (hasSome(encryptedData.getPath())) {
      return ReferencedSecretUniqueIdentifier.builder().path(encryptedData.getPath()).kmsId(kmsId).build();
    }

    return InlineSecretUniqueIdentifier.builder().encryptionKey(encryptedData.getEncryptionKey()).kmsId(kmsId).build();
  }
}
