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
