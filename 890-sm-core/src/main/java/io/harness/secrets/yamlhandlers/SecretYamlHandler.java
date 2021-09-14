/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

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
