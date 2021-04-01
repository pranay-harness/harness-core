package io.harness.jira;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.jackson.JsonNodeUtils;
import io.harness.jira.deserializer.JiraStatusDeserializer;

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
@JsonDeserialize(using = JiraStatusDeserializer.class)
public class JiraStatusNG {
  @NotNull String id;
  @NotNull String name;
  private String untranslatedName;
  private String description;
  private JiraStatusCategoryNG statusCategory;

  public JiraStatusNG(JsonNode node) {
    this.id = JsonNodeUtils.mustGetString(node, "id");
    this.name = JsonNodeUtils.mustGetString(node, "name");
    this.untranslatedName = JsonNodeUtils.getString(node, "untranslatedName");
    this.description = JsonNodeUtils.getString(node, "description");
    JsonNode statusCategory = node.get("statusCategory");
    if (statusCategory != null && statusCategory.isObject()) {
      this.statusCategory = new JiraStatusCategoryNG(statusCategory);
    }
  }
}
