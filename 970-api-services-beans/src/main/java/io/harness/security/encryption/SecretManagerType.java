package io.harness.security.encryption;

import lombok.Getter;

public enum SecretManagerType {
  KMS,
  VAULT,
  CUSTOM;

  @Getter private final String name;

  SecretManagerType() {
    this.name = name();
  }
}
