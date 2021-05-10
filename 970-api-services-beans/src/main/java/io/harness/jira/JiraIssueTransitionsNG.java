package io.harness.jira;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.jira.deserializer.JiraIssueTransitionsDeserializer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.util.ArrayList;
import java.util.List;
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
@JsonDeserialize(using = JiraIssueTransitionsDeserializer.class)
public class JiraIssueTransitionsNG {
  @NotNull List<JiraIssueTransitionNG> transitions = new ArrayList<>();

  public JiraIssueTransitionsNG(JsonNode node) {
    addTransitions(node.get("transitions"));
  }

  private void addTransitions(JsonNode node) {
    if (node == null || !node.isArray()) {
      return;
    }

    ArrayNode transitions = (ArrayNode) node;
    transitions.forEach(p -> {
      JiraIssueTransitionNG transition = new JiraIssueTransitionNG(p);
      this.transitions.add(transition);
    });
  }
}
