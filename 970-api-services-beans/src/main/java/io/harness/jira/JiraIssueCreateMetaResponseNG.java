package io.harness.jira;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.jackson.JsonNodeUtils;
import io.harness.jira.deserializer.JiraCreateMetaResponseDeserializer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.codehaus.jackson.annotate.JsonTypeName;

@OwnedBy(CDC)
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName("JiraCreateMeta")
@JsonDeserialize(using = JiraCreateMetaResponseDeserializer.class)
public class JiraIssueCreateMetaResponseNG {
  String expand;
  Map<String, JiraProjectNG> projects = new HashMap<>();

  public JiraIssueCreateMetaResponseNG(JsonNode node) {
    this.expand = JsonNodeUtils.getString(node, "expand");
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
        .filter(p -> p.getId().equals(projectKey))
        .forEach(p -> p.updateProjectStatuses(projectStatuses));
  }

  public void removeField(String fieldName) {
    this.projects.values().forEach(p -> p.removeField(fieldName));
  }
}
