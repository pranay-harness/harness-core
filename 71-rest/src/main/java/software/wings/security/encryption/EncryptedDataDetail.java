package software.wings.security.encryption;

import io.harness.security.encryption.EncryptedRecordData;
import io.harness.security.encryption.EncryptionConfig;
import io.harness.security.encryption.EncryptionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by rsingh on 10/17/17.
 */
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
