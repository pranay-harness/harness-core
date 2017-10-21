package software.wings.security.encryption;

import lombok.Builder;
import lombok.Data;
import software.wings.beans.KmsConfig;
import software.wings.security.EncryptionType;

/**
 * Created by rsingh on 10/17/17.
 */
@Data
@Builder
public class EncryptedDataDetails {
  private EncryptionType encryptionType;
  private EncryptedData encryptedData;
  private KmsConfig kmsConfig;
}
