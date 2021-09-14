/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.delegatetasks.azure.taskhandler;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@TargetModule(HarnessModule._930_DELEGATE_TASKS)
public class AzureVMSSResizeDetail {
  private String scaleSetName;
  private int desiredCount;
  private List<String> scalingPolicyJSONs;
  private boolean attachScalingPolicy;
  private String scalingPolicyMessage;
}
