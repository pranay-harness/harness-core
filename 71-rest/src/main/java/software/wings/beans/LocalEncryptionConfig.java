package software.wings.beans;

import io.harness.security.encryption.EncryptionConfig;
import io.harness.security.encryption.EncryptionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.mongodb.morphia.annotations.Transient;
import software.wings.service.intfc.security.SecretManager;

/**
 * When no other secret manager is configured. LOCAL encryption secret manager will be the default.
 * This entity don't need to be persisted in MongoDB.
 *
 * @author marklu on 2019-05-14
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class LocalEncryptionConfig implements EncryptionConfig {
  private String uuid;
  private String accountId;
  @Builder.Default private String name = SecretManager.HARNESS_DEFAULT_SECRET_MANAGER;
  @Builder.Default private EncryptionType encryptionType = EncryptionType.LOCAL;
  @Builder.Default private boolean isDefault = true;
  @Transient private int numOfEncryptedValue;
}
