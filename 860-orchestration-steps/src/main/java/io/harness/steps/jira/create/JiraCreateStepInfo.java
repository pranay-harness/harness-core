package io.harness.steps.jira.create;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.common.SwaggerConstants;
import io.harness.filters.WithConnectorRef;
import io.harness.plancreator.steps.common.SpecParameters;
import io.harness.plancreator.steps.internal.PMSStepInfo;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.OrchestrationFacilitatorType;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.steps.StepSpecTypeConstants;
import io.harness.steps.jira.JiraStepUtils;
import io.harness.steps.jira.beans.JiraField;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import java.util.HashMap;
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
@JsonTypeName(StepSpecTypeConstants.JIRA_CREATE)
@TypeAlias("jiraCreateStepInfo")
public class JiraCreateStepInfo implements PMSStepInfo, WithConnectorRef {
  @NotEmpty @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> connectorRef;
  @NotEmpty @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> projectKey;
  @NotEmpty @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> issueType;

  List<JiraField> fields;

  @Override
  public StepType getStepType() {
    return JiraCreateStep.STEP_TYPE;
  }

  @Override
  public String getFacilitatorType() {
    return OrchestrationFacilitatorType.TASK;
  }

  @Override
  public SpecParameters getSpecParameters() {
    return JiraCreateSpecParameters.builder()
        .connectorRef(connectorRef)
        .projectKey(projectKey)
        .issueType(issueType)
        .fields(JiraStepUtils.processJiraFieldsList(fields))
        .build();
  }

  @Override
  public Map<String, ParameterField<String>> extractConnectorRefs() {
    Map<String, ParameterField<String>> connectorRefMap = new HashMap<>();
    connectorRefMap.put(YAMLFieldNameConstants.CONNECTOR_REF, connectorRef);
    return connectorRefMap;
  }
}
