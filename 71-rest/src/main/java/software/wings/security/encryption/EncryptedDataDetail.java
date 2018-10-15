package software.wings.security.encryption;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.wings.security.EncryptionType;
import software.wings.service.intfc.security.EncryptionConfig;

/**
 * Created by rsingh on 10/17/17.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EncryptedDataDetail {
  // TODO: Why do we need EncryptionType separately? We have an encryptionType field in EncryptedData class. @swagat
  private EncryptionType encryptionType;
  private EncryptedData encryptedData;
  private EncryptionConfig encryptionConfig;
  private String fieldName;
}
