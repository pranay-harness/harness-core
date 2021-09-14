/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CDP)
public enum ARMSourceType {
  TEMPLATE_BODY("Template Body"),
  GIT("Git Repository");

  private final String sourceTypeLabel;
  ARMSourceType(String sourceTypeLabel) {
    this.sourceTypeLabel = sourceTypeLabel;
  }

  public static String getSourceType(String sourceType) {
    for (ARMSourceType type : values()) {
      if (type.name().equalsIgnoreCase(sourceType)) {
        return type.sourceTypeLabel;
      }
    }
    return null;
  }
}
