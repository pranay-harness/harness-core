package io.harness.pms.execution.utils;

import io.harness.pms.contracts.ambiance.ExecutionTriggerInfo;
import io.harness.pms.contracts.plan.PlanCreationContextValue;
import io.harness.pms.plan.execution.SetupAbstractionKeys;

import java.util.Map;
import lombok.experimental.UtilityClass;

@UtilityClass
public class PlanCreationContextUtils {
  public String getAccountId(Map<String, PlanCreationContextValue> globalContext) {
    return globalContext.get(SetupAbstractionKeys.accountId).getStringValue();
  }

  public String getProjectIdentifier(Map<String, PlanCreationContextValue> globalContext) {
    return globalContext.get(SetupAbstractionKeys.projectIdentifier).getStringValue();
  }

  public String getOrgIdentifier(Map<String, PlanCreationContextValue> globalContext) {
    return globalContext.get(SetupAbstractionKeys.orgIdentifier).getStringValue();
  }

  public String getPipelineIdentifier(Map<String, PlanCreationContextValue> globalContext) {
    return globalContext.get(SetupAbstractionKeys.pipelineIdentifier).getStringValue();
  }

  public ExecutionTriggerInfo getTriggerInfo(Map<String, PlanCreationContextValue> globalContext) {
    return globalContext.get(SetupAbstractionKeys.triggerInfo).getExecutionTriggerInfo();
  }

  public String getEventPayload(Map<String, PlanCreationContextValue> globalContext) {
    return globalContext.get(SetupAbstractionKeys.eventPayload).getStringValue();
  }
}
