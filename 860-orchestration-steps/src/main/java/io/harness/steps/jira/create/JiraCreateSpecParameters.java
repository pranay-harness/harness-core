package io.harness.steps.jira.create;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.ParameterField;
import io.harness.plancreator.steps.common.SpecParameters;

import java.util.Map;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(CDC)
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@TypeAlias("jiraCreateSpecParameters")
public class JiraCreateSpecParameters implements SpecParameters {
  @NotEmpty ParameterField<String> connectorRef;
  @NotEmpty ParameterField<String> projectKey;
  @NotEmpty ParameterField<String> issueType;

  Map<String, ParameterField<String>> fields;
}
