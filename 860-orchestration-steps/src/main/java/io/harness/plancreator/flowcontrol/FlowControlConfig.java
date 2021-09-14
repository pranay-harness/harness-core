/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.plancreator.flowcontrol;

import io.harness.annotation.RecasterAlias;
import io.harness.plancreator.flowcontrol.barriers.BarrierInfoConfig;

import java.util.List;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("flowControlConfig")
@RecasterAlias("io.harness.plancreator.flowcontrol.FlowControlConfig")
public class FlowControlConfig {
  @Singular List<BarrierInfoConfig> barriers;
}
