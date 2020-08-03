package io.harness.ccm.communication.entities;

import lombok.Getter;

public enum CommunicationMedium {
  EMAIL("email"),
  SLACK("slack");

  @Getter private final String name;

  CommunicationMedium(String name) {
    this.name = name;
  }
}
