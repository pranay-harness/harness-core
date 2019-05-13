package io.harness.security.encryption;

/**
 * Created by rsingh on 10/18/17.
 */
public enum EncryptionType {
  LOCAL("safeharness"),
  KMS("amazonkms"),
  AWS_SECRETS_MANAGER("awssecretsmanager"),
  VAULT("hashicorpvault");

  private final String yamlName;

  EncryptionType(String yamlName) {
    this.yamlName = yamlName;
  }

  public String getYamlName() {
    return yamlName;
  }
}
