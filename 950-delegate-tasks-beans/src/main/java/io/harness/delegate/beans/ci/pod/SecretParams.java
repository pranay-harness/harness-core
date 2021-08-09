package io.harness.delegate.beans.ci.pod;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SecretParams {
  public enum Type { FILE, TEXT }
  private String value;
  private String secretKey;
  private Type type;
}
