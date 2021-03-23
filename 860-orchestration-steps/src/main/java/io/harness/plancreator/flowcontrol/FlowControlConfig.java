package io.harness.plancreator.flowcontrol;

import io.harness.plancreator.flowcontrol.barriers.BarrierInfoConfig;
import io.harness.plancreator.flowcontrol.resourceconstraints.ResourceConstraintInfoConfig;

import java.util.List;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("flowControlConfig")
public class FlowControlConfig {
  @Singular List<BarrierInfoConfig> barriers;
  @Singular List<ResourceConstraintInfoConfig> resourceConstraints;
}
