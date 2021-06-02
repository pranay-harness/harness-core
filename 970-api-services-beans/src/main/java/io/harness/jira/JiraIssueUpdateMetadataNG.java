package io.harness.jira;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.jira.deserializer.JiraIssueUpdateMetadataDeserializer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.HashMap;
import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@OwnedBy(CDC)
@Data
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDeserialize(using = JiraIssueUpdateMetadataDeserializer.class)
public class JiraIssueUpdateMetadataNG {
  @NotNull Map<String, JiraFieldNG> fields = new HashMap<>();

  public JiraIssueUpdateMetadataNG(JsonNode node) {
    addFields(node.get("fields"));
  }

  private void addFields(JsonNode node) {
    if (node == null || !node.isObject()) {
      return;
    }

    ObjectNode fields = (ObjectNode) node;
    fields.fields().forEachRemaining(f -> JiraFieldNG.addFields(this.fields, f.getKey(), f.getValue()));
  }
}
