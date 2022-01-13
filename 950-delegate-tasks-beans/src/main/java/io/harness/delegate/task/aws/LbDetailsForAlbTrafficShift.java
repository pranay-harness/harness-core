/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
