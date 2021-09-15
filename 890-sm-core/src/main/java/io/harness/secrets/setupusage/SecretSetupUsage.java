/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.secrets.setupusage;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.persistence.UuidAware;

import software.wings.settings.SettingVariableTypes;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;

@OwnedBy(PL)
@Value
@Builder
@EqualsAndHashCode(exclude = "entity")
public class SecretSetupUsage {
  @NonNull private String entityId;
  @NonNull private SettingVariableTypes type;
  private String fieldName;
  private UuidAware entity;
}
