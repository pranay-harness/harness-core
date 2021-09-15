/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.jira;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CDC)
public enum JiraActionNG {
  VALIDATE_CREDENTIALS("Validate Credentials"),
  GET_PROJECTS("Get Projects"),
  GET_STATUSES("Get Statuses"),
  GET_ISSUE("Get Issue"),
  GET_ISSUE_CREATE_METADATA("Get Issue Create Metadata"),
  GET_ISSUE_UPDATE_METADATA("Get Issue Update Metadata"),
  CREATE_ISSUE("Create Issue"),
  UPDATE_ISSUE("Update Issue");

  private final String displayName;

  JiraActionNG(String s) {
    displayName = s;
  }

  public String getDisplayName() {
    return displayName;
  }
}
