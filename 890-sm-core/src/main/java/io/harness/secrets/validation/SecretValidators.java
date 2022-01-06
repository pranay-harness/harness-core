/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.secrets.validation;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import lombok.Getter;

@OwnedBy(PL)
public enum SecretValidators {
  AWS_SECRET_MANAGAER_VALIDATOR,
  GCP_SECRET_MANAGER_VALIDATOR,
  AZURE_SECRET_MANAGER_VALIDATOR,
  VAULT_SECRET_MANAGER_VALIDATOR,
  COMMON_SECRET_MANAGER_VALIDATOR;

  @Getter private final String name;

  SecretValidators() {
    this.name = name();
  }
}
