package software.wings.service.intfc.security;

import software.wings.security.EncryptionType;

/**
 * Created by rsingh on 11/3/17.
 */
public interface EncryptionConfig {
  String getUuid();
  EncryptionType getEncryptionType();
  boolean isDefault();
}
