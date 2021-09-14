/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cdng.manifest.yaml;

import io.harness.helm.HelmSubCommandType;

import lombok.Getter;

public enum HelmCommandFlagType {
  Fetch(HelmSubCommandType.FETCH),
  Version(HelmSubCommandType.VERSION),
  Template(HelmSubCommandType.TEMPLATE),
  Pull(HelmSubCommandType.PULL);

  @Getter private HelmSubCommandType subCommandType;

  HelmCommandFlagType(HelmSubCommandType subCommandType) {
    this.subCommandType = subCommandType;
  }
}
