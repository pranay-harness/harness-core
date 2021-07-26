package io.harness.manifest;

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
