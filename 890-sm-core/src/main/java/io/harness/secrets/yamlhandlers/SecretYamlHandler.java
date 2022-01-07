/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
