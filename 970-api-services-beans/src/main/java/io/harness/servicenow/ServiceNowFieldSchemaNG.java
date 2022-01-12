/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.servicenow;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.jackson.JsonNodeUtils;

import com.fasterxml.jackson.databind.JsonNode;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@OwnedBy(CDC)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ServiceNowFieldSchemaNG {
  boolean array;
  @NotNull String typeStr;
  @NotNull ServiceNowFieldTypeNG type;
  String customType;

  public ServiceNowFieldSchemaNG(JsonNode node) {
    this.typeStr = JsonNodeUtils.getString(node, "type", "string");
    if (typeStr.equals("array")) {
      this.array = true;
      this.typeStr = JsonNodeUtils.getString(node, "items", "string");
    } else {
      this.array = false;
    }

    this.type = ServiceNowFieldTypeNG.fromTypeString(typeStr);

    this.customType = JsonNodeUtils.getString(node, "custom", null);
  }
}
