package io.harness.delegate.beans.ci.pod;

import io.harness.security.encryption.EncryptedDataDetail;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EncryptedVariableWithType {
  public enum Type { FILE, TEXT }
  private EncryptedDataDetail encryptedDataDetail;
  private Type type;
}
