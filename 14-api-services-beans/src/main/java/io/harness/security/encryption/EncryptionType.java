package io.harness.security.encryption;

/**
 * Created by rsingh on 10/18/17.
 */
public enum EncryptionType {
  LOCAL("safeharness"),
  KMS("amazonkms"),
  GCP_KMS("gcpkms"),
  AWS_SECRETS_MANAGER("awssecretsmanager"),
  AZURE_VAULT("azurevault"),
  CYBERARK("cyberark"),
  VAULT("hashicorpvault"),
  CUSTOM("custom");

  private final String yamlName;

  EncryptionType(String yamlName) {
    this.yamlName = yamlName;
  }

  public String getYamlName() {
    return yamlName;
  }
}
