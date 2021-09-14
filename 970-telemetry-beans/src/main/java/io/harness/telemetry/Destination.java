/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.telemetry;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.GTM)
public enum Destination {
  NATERO("Natero"),
  MARKETO("Marketo"),
  SALESFORCE("Salesforce"),
  AMPLITUDE("Amplitude"),
  ALL("All");

  private String destinationName;

  Destination(String destinationName) {
    this.destinationName = destinationName;
  }

  public String getDestinationName() {
    return destinationName;
  }
}
