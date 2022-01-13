/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
