package io.harness.ccm.communication.entities;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;

import lombok.Getter;

@OwnedBy(CE)
public enum CommunicationMedium {
  EMAIL("email"),
  SLACK("slack");

  @Getter private final String name;

  CommunicationMedium(String name) {
    this.name = name;
  }
}
