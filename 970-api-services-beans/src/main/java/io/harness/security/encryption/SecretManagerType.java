/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.security.encryption;

import lombok.Getter;

public enum SecretManagerType {
  KMS,
  VAULT,
  CUSTOM,
  SSH;

  @Getter private final String name;

  SecretManagerType() {
    this.name = name();
  }
}
