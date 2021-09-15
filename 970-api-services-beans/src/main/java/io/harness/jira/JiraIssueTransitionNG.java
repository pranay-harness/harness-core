/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.jira;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.jackson.JsonNodeUtils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@OwnedBy(CDC)
@Data
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class JiraIssueTransitionNG {
  @NotNull String id;
  @NotNull String name;
  @NotNull JiraStatusNG to;
  boolean isAvailable;

  public JiraIssueTransitionNG(JsonNode node) {
    this.id = JsonNodeUtils.mustGetString(node, "id");
    this.name = JsonNodeUtils.mustGetString(node, "name");
    this.to = new JiraStatusNG(node.get("to"));
    this.isAvailable = JsonNodeUtils.getBoolean(node, "isAvailable", true);
  }
}
