/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@OwnedBy(PL)
public enum ModuleType {
  @JsonProperty("CD") CD("Continuous Deployment"),
  @JsonProperty("CI") CI("Continuous Integration"),
  @JsonProperty("CV") CV("Continuous Verification"),
  @JsonProperty("CF") CF("Continuous Features"),
  @JsonProperty("CE") CE("Continuous Efficiency"),

  // Internal
  @JsonProperty("CORE") CORE("Core", true),
  @JsonProperty("PMS") PMS("Pipelines", true),
  @JsonProperty("TEMPLATESERVICE") TEMPLATESERVICE("TemplateService", true);

  String displayName;
  boolean internal;

  ModuleType(String displayName) {
    this(displayName, false);
  }

  ModuleType(String displayName, boolean internal) {
    this.displayName = displayName;
    this.internal = internal;
  }

  @JsonCreator
  public static ModuleType fromString(String moduleType) {
    for (ModuleType moduleEnum : ModuleType.values()) {
      if (moduleEnum.name().equalsIgnoreCase(moduleType)) {
        return moduleEnum;
      }
    }
    throw new IllegalArgumentException("Invalid value: " + moduleType);
  }

  public String getDisplayName() {
    return displayName;
  }

  public boolean isInternal() {
    return internal;
  }
}
