package io.harness.steps.approval.step.jira;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SwaggerConstants;
import io.harness.filters.WithConnectorRef;
import io.harness.plancreator.steps.common.SpecParameters;
import io.harness.plancreator.steps.internal.PMSStepInfo;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.steps.StepSpecTypeConstants;
import io.harness.steps.approval.ApprovalFacilitator;
import io.harness.steps.approval.step.jira.beans.CriteriaSpecWrapper;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import java.util.HashMap;
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
@JsonTypeName(StepSpecTypeConstants.JIRA_APPROVAL)
@TypeAlias("jiraApprovalStepInfo")
public class JiraApprovalStepInfo implements PMSStepInfo, WithConnectorRef {
  @NotNull @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> connectorRef;
  @NotNull @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> issueKey;
  @NotNull CriteriaSpecWrapper approvalCriteria;
  CriteriaSpecWrapper rejectionCriteria;

  @Override
  public StepType getStepType() {
    return JiraApprovalStep.STEP_TYPE;
  }

  @Override
  public String getFacilitatorType() {
    return ApprovalFacilitator.APPROVAL_FACILITATOR;
  }

  @Override
  public SpecParameters getSpecParameters() {
    return JiraApprovalSpecParameters.builder()
        .connectorRef(connectorRef)
        .issueKey(issueKey)
        .approvalCriteria(approvalCriteria)
        .rejectionCriteria(rejectionCriteria)
        .build();
  }

  @Override
  public Map<String, ParameterField<String>> extractConnectorRefs() {
    Map<String, ParameterField<String>> connectorRefMap = new HashMap<>();
    connectorRefMap.put(YAMLFieldNameConstants.CONNECTOR_REF, connectorRef);
    return connectorRefMap;
  }
}
