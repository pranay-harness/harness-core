package io.harness.delegate.beans;

import io.harness.security.encryption.EncryptedRecord;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SecretDetail {
  private String configUuid;
  private EncryptedRecord encryptedRecord;
}
