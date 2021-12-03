package io.harness.cdng.pipeline;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.plancreator.steps.AbstractStepNode;
import io.harness.yaml.core.failurestrategy.FailureStrategyConfig;

import java.util.List;

@OwnedBy(PIPELINE)
public abstract class CdAbstractStepNode extends AbstractStepNode {
  List<FailureStrategyConfig> failureStrategies;
}
