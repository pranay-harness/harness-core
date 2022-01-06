/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

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
