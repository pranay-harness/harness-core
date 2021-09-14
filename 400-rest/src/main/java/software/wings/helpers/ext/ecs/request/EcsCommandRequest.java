/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.helpers.ext.ecs.request;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.expression.ExpressionEvaluator;

import software.wings.beans.AwsConfig;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

@Data
@AllArgsConstructor
@TargetModule(HarnessModule._950_DELEGATE_TASKS_BEANS)
public class EcsCommandRequest implements ExecutionCapabilityDemander {
  private String accountId;
  private String appId;
  private String commandName;
  private String activityId;
  private String region;
  private String cluster;
  private AwsConfig AwsConfig;
  @NotEmpty private EcsCommandType EcsCommandType;
  private boolean timeoutErrorSupported;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    return AwsConfig.fetchRequiredExecutionCapabilities(maskingEvaluator);
  }

  public enum EcsCommandType {
    LISTENER_UPDATE_BG,
    BG_SERVICE_SETUP,
    SERVICE_SETUP,
    ECS_RUN_TASK_DEPLOY,
    ROUTE53_BG_SERVICE_SETUP,
    ROUTE53_DNS_WEIGHT_UPDATE,
    SERVICE_DEPLOY,
    DEPLOY_ROLLBACK_DATA_FETCH
  }
}
