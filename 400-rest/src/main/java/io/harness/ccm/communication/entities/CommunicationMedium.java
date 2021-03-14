package io.harness.ccm.communication.entities;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import lombok.Getter;
@TargetModule(Module._490_CE_COMMONS)
public enum CommunicationMedium {
  EMAIL("email"),
  SLACK("slack");

  @Getter private final String name;

  CommunicationMedium(String name) {
    this.name = name;
  }
}
