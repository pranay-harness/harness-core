package io.harness.steps.jira.update;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
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
import io.harness.steps.jira.update.beans.TransitionTo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import java.util.stream.Collectors;
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
@JsonTypeName(StepSpecTypeConstants.JIRA_UPDATE)
@TypeAlias("jiraUpdateStepInfo")
public class JiraUpdateStepInfo implements PMSStepInfo, WithRollbackInfo {
  @JsonIgnore String name;
  @JsonIgnore String identifier;

  @NotNull @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> connectorRef;
  @NotNull @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> issueKey;

  TransitionTo transitionTo;
  List<JiraField> fields;

  @Override
  public StepType getStepType() {
    return JiraUpdateStep.STEP_TYPE;
  }

  @Override
  public String getFacilitatorType() {
    return OrchestrationFacilitatorType.TASK;
  }

  @Override
  public StepParameters getStepParametersWithRollbackInfo(BaseStepParameterInfo baseStepParameterInfo) {
    return JiraUpdateStepParameters.builder()
        .name(name)
        .identifier(identifier)
        .timeout(baseStepParameterInfo.getTimeout())
        .connectorRef(connectorRef)
        .issueKey(issueKey)
        .transitionTo(transitionTo)
        .fields(
            fields == null ? null : fields.stream().collect(Collectors.toMap(JiraField::getName, JiraField::getValue)))
        .build();
  }

  @Override
  public boolean validateStageFailureStrategy() {
    return false;
  }
}
