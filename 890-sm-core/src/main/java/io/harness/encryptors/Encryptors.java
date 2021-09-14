/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.encryptors;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import lombok.Getter;

@OwnedBy(PL)
public enum Encryptors {
  LOCAL_ENCRYPTOR,
  AWS_KMS_ENCRYPTOR,
  GCP_KMS_ENCRYPTOR,
  GLOBAL_AWS_KMS_ENCRYPTOR,
  GLOBAL_GCP_KMS_ENCRYPTOR,
  HASHICORP_VAULT_ENCRYPTOR,
  AWS_VAULT_ENCRYPTOR,
  AZURE_VAULT_ENCRYPTOR,
  GCP_VAULT_ENCRYPTOR,
  CYBERARK_VAULT_ENCRYPTOR,
  CUSTOM_ENCRYPTOR;

  @Getter private final String name;

  Encryptors() {
    this.name = name();
  }
}
