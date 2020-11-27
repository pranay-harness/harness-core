package io.harness.secrets.validation;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import lombok.Getter;

@OwnedBy(PL)
public enum SecretValidators {
  AWS_SECRET_MANAGAER_VALIDATOR,
  AZURE_SECRET_MANAGER_VALIDATOR,
  VAULT_SECRET_MANAGER_VALIDATOR,
  COMMON_SECRET_MANAGER_VALIDATOR;

  @Getter private final String name;

  SecretValidators() {
    this.name = name();
  }
}
