/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.task.aws;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LbDetailsForAlbTrafficShift {
  private String loadBalancerName;
  private String loadBalancerArn;
  private String listenerPort;
  private String listenerArn;
  private boolean useSpecificRule;
  private String ruleArn;
  private String prodTargetGroupName;
  private String prodTargetGroupArn;
  private String stageTargetGroupName;
  private String stageTargetGroupArn;
}
