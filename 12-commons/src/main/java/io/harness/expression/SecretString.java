package io.harness.expression;

import lombok.Builder;

@Builder
public class SecretString {
  private String value;

  @Override
  public String toString() {
    return value;
  }
}
