package io.harness.encryption;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class SecretRefData {
  private String identifier;
  private Scope scope;
  private char[] decryptedValue;
}