package io.harness.ccm.communication.entities;

import lombok.Getter;

public enum CommunicationSource {
  EMAIL("email"),
  SLACK("slack");

  @Getter private final String name;

  CommunicationSource(String name) {
    this.name = name;
  }
}
