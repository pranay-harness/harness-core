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
