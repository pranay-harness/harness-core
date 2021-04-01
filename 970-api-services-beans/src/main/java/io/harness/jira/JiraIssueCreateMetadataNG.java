package io.harness.jira;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.jira.deserializer.JiraCreateMetadataDeserializer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.codehaus.jackson.annotate.JsonTypeName;

@OwnedBy(CDC)
@Data
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName("JiraCreateMeta")
@JsonDeserialize(using = JiraCreateMetadataDeserializer.class)
public class JiraIssueCreateMetadataNG {
  Map<String, JiraProjectNG> projects = new HashMap<>();

  public JiraIssueCreateMetadataNG(JsonNode node) {
    addProjects(node.get("projects"));
  }

  private void addProjects(JsonNode node) {
    if (node == null || !node.isArray()) {
      return;
    }

    ArrayNode projects = (ArrayNode) node;
    projects.forEach(p -> {
      JiraProjectNG project = new JiraProjectNG(p);
      this.projects.put(project.getKey(), project);
    });
  }

  public void updateStatuses(List<JiraStatusNG> statuses) {
    if (EmptyPredicate.isEmpty(statuses)) {
      return;
    }
    this.projects.values().forEach(p -> p.updateStatuses(statuses));
  }

  public void updateProjectStatuses(String projectKey, List<JiraIssueTypeNG> projectStatuses) {
    if (EmptyPredicate.isEmpty(projectStatuses)) {
      return;
    }
    this.projects.values()
        .stream()
        .filter(p -> p.getKey().equals(projectKey))
        .forEach(p -> p.updateProjectStatuses(projectStatuses));
  }

  public void removeField(String fieldName) {
    this.projects.values().forEach(p -> p.removeField(fieldName));
  }
}
