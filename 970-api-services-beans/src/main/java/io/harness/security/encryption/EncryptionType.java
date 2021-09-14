/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.security.encryption;

public enum EncryptionType {
  LOCAL("safeharness"),
  KMS("amazonkms"),
  GCP_KMS("gcpkms"),
  AWS_SECRETS_MANAGER("awssecretsmanager"),
  AZURE_VAULT("azurevault"),
  CYBERARK("cyberark"),
  VAULT("hashicorpvault"),
  GCP_SECRETS_MANAGER("gcpsecretsmanager"),
  CUSTOM("custom"),
  VAULT_SSH("vaultssh");

  private final String yamlName;

  EncryptionType(String yamlName) {
    this.yamlName = yamlName;
  }

  public String getYamlName() {
    return yamlName;
  }
}
