package io.harness.servicenow;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.jackson.JsonNodeUtils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@OwnedBy(CDC)
@Data
@Builder
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ServiceNowFieldNG {
  @NotNull String key;
  @NotNull String name;
  boolean required;
  boolean isCustom;
  @NotNull ServiceNowFieldSchemaNG schema;
  @Builder.Default @NotNull List<ServiceNowFieldAllowedValueNG> allowedValues = new ArrayList<>();

  public ServiceNowFieldNG() {
    this.allowedValues = new ArrayList<>();
  }

  private ServiceNowFieldNG(String key, JsonNode node) {
    this.key = JsonNodeUtils.getString(node, "key", key);
    this.name = JsonNodeUtils.mustGetString(node, "name");
    this.required = JsonNodeUtils.getBoolean(node, "required", false);
    this.isCustom = this.key.startsWith("customfield_");
    this.schema = new ServiceNowFieldSchemaNG(node.get("schema"));
    this.allowedValues = new ArrayList<>();
    addAllowedValues(node.get("allowedValues"));
  }

  private void addAllowedValues(JsonNode node) {
    if (node == null || !node.isArray()) {
      return;
    }

    ArrayNode allowedValues = (ArrayNode) node;
    allowedValues.forEach(av -> this.allowedValues.add(new ServiceNowFieldAllowedValueNG(av)));
  }
}
