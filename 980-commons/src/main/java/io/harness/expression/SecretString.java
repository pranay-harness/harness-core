package io.harness.expression;

import lombok.Builder;

@Builder
public class SecretString {
  public static final String SECRET_MASK = "**************";

  private String value;

  @Override
  public String toString() {
    return value;
  }
}
