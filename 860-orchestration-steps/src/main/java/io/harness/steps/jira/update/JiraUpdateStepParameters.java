package io.harness.steps.jira.update;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.yaml.ParameterField;
import io.harness.steps.jira.update.beans.TransitionTo;

import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(CDC)
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@TypeAlias("jiraUpdateStepParameters")
public class JiraUpdateStepParameters implements StepParameters {
  String name;
  String identifier;
  ParameterField<String> timeout;

  @NotNull ParameterField<String> connectorRef;
  @NotNull ParameterField<String> issueKey;

  TransitionTo transitionTo;
  Map<String, ParameterField<String>> fields;
}
