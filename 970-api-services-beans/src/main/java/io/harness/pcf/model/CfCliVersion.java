/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pcf.model;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidArgumentsException;

import org.apache.commons.lang3.StringUtils;

@OwnedBy(HarnessTeam.CDP)
public enum CfCliVersion {
  V6,
  V7;

  public static CfCliVersion fromString(final String version) {
    if (StringUtils.isBlank(version)) {
      return null;
    }

    if (version.charAt(0) == '7') {
      return V7;
    } else if (version.charAt(0) == '6') {
      return V6;
    } else {
      throw new InvalidArgumentsException(String.format("Unsupported CF CLI version, version: %s", version));
    }
  }
}
