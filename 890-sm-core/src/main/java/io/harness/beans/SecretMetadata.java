package io.harness.beans;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@Builder
@EqualsAndHashCode
public class SecretMetadata {
  private String secretId;
  private SecretState secretState;
}
