package io.harness.pcf.model;

import io.harness.exception.InvalidArgumentsException;

import org.apache.commons.lang3.StringUtils;

public enum PcfCliVersion {
  V6,
  V7;

  public static PcfCliVersion fromString(final String version) {
    if (StringUtils.isBlank(version)) {
      return null;
    }

    if (version.startsWith("7")) {
      return V7;
    } else if (version.startsWith("6")) {
      return V6;
    } else {
      throw new InvalidArgumentsException(String.format("Unsupported CF CLI version, version: %s", version));
    }
  }
}
