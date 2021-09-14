/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.secrets.setupusage;

import lombok.Getter;

public enum SecretSetupUsageBuilders {
  SETTING_ATTRIBUTE_SETUP_USAGE_BUILDER,
  SERVICE_VARIABLE_SETUP_USAGE_BUILDER,
  SECRET_MANAGER_CONFIG_SETUP_USAGE_BUILDER,
  CONFIG_FILE_SETUP_USAGE_BUILDER,
  TRIGGER_SETUP_USAGE_BUILDER;

  @Getter private String name;

  SecretSetupUsageBuilders() {
    this.name = name();
  }
}
