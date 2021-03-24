package io.harness.jira;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.HashMap;
import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@OwnedBy(CDC)
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class JiraUpdateIssueRequestNG {
  JiraIssueTransitionRequestNG transition;
  @NotNull Map<String, Object> fields = new HashMap<>();

  public JiraUpdateIssueRequestNG(
      JiraIssueUpdateMetadataNG updateMetadata, String transitionId, Map<String, String> fields) {
    if (EmptyPredicate.isNotEmpty(transitionId)) {
      this.transition = JiraIssueTransitionRequestNG.builder().id(transitionId).build();
    }
    JiraCreateUpdateIssueUtils.updateFieldValues(this.fields, updateMetadata.getFields(), fields);
  }
}
