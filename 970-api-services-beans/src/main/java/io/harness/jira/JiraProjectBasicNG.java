/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.jira;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.jackson.JsonNodeUtils;
import io.harness.jira.deserializer.JiraProjectBasicDeserializer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
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
@JsonDeserialize(using = JiraProjectBasicDeserializer.class)
public class JiraProjectBasicNG {
  @NotNull String id;
  @NotNull String key;
  @NotNull String name;

  public JiraProjectBasicNG(JsonNode node) {
    this.id = JsonNodeUtils.mustGetString(node, "id");
    this.key = JsonNodeUtils.mustGetString(node, "key");
    this.name = JsonNodeUtils.getString(node, "name");
    if (this.name == null) {
      this.name = this.key;
    }
  }
}
