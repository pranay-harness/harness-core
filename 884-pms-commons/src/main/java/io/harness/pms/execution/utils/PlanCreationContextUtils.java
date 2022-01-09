/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.pms.execution.utils;

import io.harness.pms.contracts.plan.ExecutionTriggerInfo;
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
    return globalContext.get(SetupAbstractionKeys.triggerInfo).getMetadata().getTriggerInfo();
  }

  public String getEventPayload(Map<String, PlanCreationContextValue> globalContext) {
    return globalContext.get(SetupAbstractionKeys.eventPayload).getStringValue();
  }
}
