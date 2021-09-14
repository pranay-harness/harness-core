/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.helpers.ext.ecs.request;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@TargetModule(HarnessModule._950_DELEGATE_TASKS_BEANS)
@OwnedBy(CDP)
public class EcsListenerUpdateRequestConfigData {
  private String prodListenerArn;
  private String stageListenerArn;
  private boolean isUseSpecificRuleArn;
  private String prodListenerRuleArn;
  private String stageListenerRuleArn;
  private String serviceName;
  private String clusterName;
  private String region;
  private String serviceNameDownsized;
  private int serviceCountDownsized;
  private boolean rollback;
  private boolean downsizeOldService;
  private long downsizeOldServiceDelayInSecs;
  private String targetGroupForNewService;
  private String targetGroupForExistingService;
}
