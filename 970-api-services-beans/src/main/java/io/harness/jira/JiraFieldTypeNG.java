/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.jira;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidArgumentsException;

import com.fasterxml.jackson.annotation.JsonProperty;

@OwnedBy(CDC)
public enum JiraFieldTypeNG {
  @JsonProperty("string") STRING,
  @JsonProperty("number") NUMBER,
  @JsonProperty("date") DATE,
  @JsonProperty("datetime") DATETIME,
  @JsonProperty("timetracking") TIME_TRACKING,
  @JsonProperty("option") OPTION;

  public static JiraFieldTypeNG fromTypeString(String typeStr) {
    if (typeStr == null) {
      throw new InvalidArgumentsException("Empty type");
    }

    switch (typeStr) {
      case "any":
      case "string":
        return STRING;
      case "number":
        return NUMBER;
      case "date":
        return DATE;
      case "datetime":
        return DATETIME;
      case "timetracking":
        return TIME_TRACKING;
      case "option":
      case "resolution":
      case "component":
      case "priority":
      case "version":
        return OPTION;
      default:
        // Special fields (project, issuetype) and unknown fields throw this exception and are not part of issue create
        // meta.
        throw new InvalidArgumentsException(String.format("Unsupported type: %s", typeStr));
    }
  }
}
