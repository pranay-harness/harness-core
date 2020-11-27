package io.harness.secrets.yamlhandlers;

import io.harness.beans.EncryptedData;

import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotEmpty;

public interface SecretYamlHandler {
  String URL_ROOT_PREFIX = "//";
  String YAML_PREFIX = "YAML_";
  String toYaml(@NotEmpty String accountId, @NotEmpty String secretId);
  String toYaml(@NotNull EncryptedData encryptedData);
  EncryptedData fromYaml(@NotEmpty String accountId, @NotEmpty String yamlRef);
}
