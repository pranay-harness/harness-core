/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.jira;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.ToString;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder
public class JiraInternalConfig {
  String jiraUrl;
  String username;
  @ToString.Exclude String password;

  public String getJiraUrl() {
    return jiraUrl.endsWith("/") ? jiraUrl : jiraUrl.concat("/");
  }
}
