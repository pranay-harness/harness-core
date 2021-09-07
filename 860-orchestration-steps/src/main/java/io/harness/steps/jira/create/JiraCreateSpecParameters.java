package io.harness.steps.jira.create;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.plancreator.steps.common.SpecParameters;
import io.harness.pms.yaml.ParameterField;

import java.util.List;
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
@RecasterAlias("io.harness.steps.jira.create.JiraCreateSpecParameters")
public class JiraCreateSpecParameters implements SpecParameters {
  @NotEmpty ParameterField<String> connectorRef;
  @NotEmpty ParameterField<String> projectKey;
  @NotEmpty ParameterField<String> issueType;

  Map<String, ParameterField<String>> fields;
  ParameterField<List<String>> delegateSelectors;
}
