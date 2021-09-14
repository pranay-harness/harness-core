/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.task.aws;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AwsElbListenerRuleData {
  private String ruleArn;
  private String rulePriority;
  private String ruleTargetGroupArn;
  boolean isDefault;
}
