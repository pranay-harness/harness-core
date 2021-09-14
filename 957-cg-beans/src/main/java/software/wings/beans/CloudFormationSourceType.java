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
public enum CloudFormationSourceType {
  TEMPLATE_BODY("Template Body"),
  TEMPLATE_URL("Amazon S3"),
  GIT("Git Repository"),
  UNKNOWN("Unknown");

  private final String sourceTypeLabel;

  CloudFormationSourceType(String sourceTypeLabel) {
    this.sourceTypeLabel = sourceTypeLabel;
  }

  public static String getSourceType(String sourceType) {
    String sourceTypeLabel = UNKNOWN.sourceTypeLabel;
    for (CloudFormationSourceType type : values()) {
      if (type.name().equalsIgnoreCase(sourceType)) {
        sourceTypeLabel = type.sourceTypeLabel;
        break;
      }
    }
    return sourceTypeLabel;
  }
}
