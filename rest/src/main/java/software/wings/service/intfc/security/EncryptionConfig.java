package software.wings.service.intfc.security;

import software.wings.beans.UuidAware;
import software.wings.security.EncryptionType;

/**
 * Created by rsingh on 11/3/17.
 */
public interface EncryptionConfig extends UuidAware {
  EncryptionType getEncryptionType();

  boolean isDefault();
}
