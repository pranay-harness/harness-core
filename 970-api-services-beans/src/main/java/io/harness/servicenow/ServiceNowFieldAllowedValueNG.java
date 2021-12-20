package io.harness.servicenow;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.jackson.JsonNodeUtils;
import io.harness.jira.JiraStatusNG;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ServiceNowFieldAllowedValueNG {
  String id;
  String name;
  String value;

  public ServiceNowFieldAllowedValueNG(JsonNode node) {
    this.id = JsonNodeUtils.getString(node, "id");
    this.name = JsonNodeUtils.getString(node, "name");
    this.value = JsonNodeUtils.getString(node, "value");
  }

  public boolean matchesValue(String value) {
    return value != null && (value.equals(this.id) || value.equals(this.name) || value.equals(this.value));
  }

  public String displayValue() {
    return value == null ? (name == null ? id : name) : value;
  }

  public static ServiceNowFieldAllowedValueNG fromStatus(JiraStatusNG status) {
    return ServiceNowFieldAllowedValueNG.builder().id(status.getId()).name(status.getName()).build();
  }
}
