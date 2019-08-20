package io.harness.security.encryption;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EncryptedDataDetail {
  // TODO: Why do we need EncryptionType separately? We have an encryptionType field in EncryptedData class. @swagat
  private EncryptionType encryptionType;
  private EncryptedRecordData encryptedData;
  private EncryptionConfig encryptionConfig;
  private String fieldName;
}
