/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.steps.approval.step.jira.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@OwnedBy(CDC)
@FieldDefaults(level = AccessLevel.PRIVATE)
public enum Operator {
  @JsonProperty("equals") EQ("equals"),
  @JsonProperty("not equals") NOT_EQ("not equals"),
  @JsonProperty("in") IN("in"),
  @JsonProperty("not in") NOT_IN("not in");

  @Getter final String displayName;

  Operator(String displayName) {
    this.displayName = displayName;
  }

  @Override
  public String toString() {
    return displayName;
  }
}
