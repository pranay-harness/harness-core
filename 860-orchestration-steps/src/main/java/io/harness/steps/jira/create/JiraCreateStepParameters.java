package io.harness.steps.jira.create;

import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.yaml.ParameterField;

import java.util.Map;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@TypeAlias("jiraCreateStepParameters")
public class JiraCreateStepParameters implements StepParameters {
  String name;
  String identifier;
  ParameterField<String> timeout;

  @NotEmpty ParameterField<String> connectorRef;
  @NotEmpty ParameterField<String> projectKey;
  @NotEmpty ParameterField<String> issueType;

  Map<String, ParameterField<String>> fields;
}
