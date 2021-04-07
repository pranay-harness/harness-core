package io.harness.ccm.communication.entities;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import lombok.Getter;

@OwnedBy(CE)
@TargetModule(HarnessModule._490_CE_COMMONS)
public enum CommunicationMedium {
  EMAIL("email"),
  SLACK("slack");

  @Getter private final String name;

  CommunicationMedium(String name) {
    this.name = name;
  }
}
