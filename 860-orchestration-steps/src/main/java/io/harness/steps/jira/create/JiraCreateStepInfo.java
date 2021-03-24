package io.harness.steps.jira.create;

import io.harness.common.SwaggerConstants;
import io.harness.plancreator.steps.internal.PMSStepInfo;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.facilitator.OrchestrationFacilitatorType;
import io.harness.pms.sdk.core.steps.io.BaseStepParameterInfo;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.sdk.core.steps.io.WithRollbackInfo;
import io.harness.pms.yaml.ParameterField;
import io.harness.steps.StepSpecTypeConstants;
import io.harness.steps.jira.beans.JiraField;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonTypeName(StepSpecTypeConstants.JIRA_CREATE)
@TypeAlias("jiraCreateStepInfo")
public class JiraCreateStepInfo implements PMSStepInfo, WithRollbackInfo {
  @JsonIgnore String name;
  @JsonIgnore String identifier;

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
  public StepParameters getStepParametersWithRollbackInfo(BaseStepParameterInfo baseStepParameterInfo) {
    return JiraCreateStepParameters.builder()
        .name(name)
        .identifier(identifier)
        .connectorRef(connectorRef)
        .projectKey(projectKey)
        .issueType(issueType)
        .fields(
            fields == null ? null : fields.stream().collect(Collectors.toMap(JiraField::getName, JiraField::getValue)))
        .build();
  }

  @Override
  public boolean validateStageFailureStrategy() {
    return false;
  }
}
