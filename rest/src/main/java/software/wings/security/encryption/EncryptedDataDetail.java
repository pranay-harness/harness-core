package software.wings.security.encryption;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.wings.beans.KmsConfig;
import software.wings.security.EncryptionType;

import java.lang.reflect.Field;

/**
 * Created by rsingh on 10/17/17.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EncryptedDataDetail {
  private EncryptionType encryptionType;
  private EncryptedData encryptedData;
  private KmsConfig kmsConfig;
  private String fieldName;
}
