package io.harness.serializer.spring;

import io.harness.spring.AliasRegistrar;
import io.harness.steps.barriers.BarrierStepParameters;
import io.harness.steps.barriers.beans.BarrierExecutionInstance;
import io.harness.steps.barriers.beans.BarrierOutcome;
import io.harness.steps.dummy.DummySectionOutcome;
import io.harness.steps.dummy.DummySectionStepParameters;
import io.harness.steps.dummy.DummySectionStepTransput;
import io.harness.steps.dummy.DummyStepParameters;
import io.harness.steps.fork.ForkStepParameters;
import io.harness.steps.resourcerestraint.ResourceRestraintStepParameters;
import io.harness.steps.resourcerestraint.beans.ResourceRestraintInstance;
import io.harness.steps.resourcerestraint.beans.ResourceRestraintOutcome;
import io.harness.steps.section.SectionStepParameters;
import io.harness.steps.section.chain.SectionChainPassThroughData;
import io.harness.steps.section.chain.SectionChainStepParameters;

import java.util.Map;

public class OrchestrationStepsAliasRegistrar implements AliasRegistrar {
  @Override
  public void register(Map<String, Class<?>> orchestrationElements) {
    orchestrationElements.put("barrierExecutionInstance", BarrierExecutionInstance.class);
    orchestrationElements.put("barrierStepParameters", BarrierStepParameters.class);
    orchestrationElements.put("barrierOutcome", BarrierOutcome.class);
    orchestrationElements.put("resourceRestraintInstance", ResourceRestraintInstance.class);
    orchestrationElements.put("resourceRestraintOutcome", ResourceRestraintOutcome.class);
    orchestrationElements.put("resourceRestraintStepParameters", ResourceRestraintStepParameters.class);
    orchestrationElements.put("dummySectionOutcome", DummySectionOutcome.class);
    orchestrationElements.put("dummySectionStepParameters", DummySectionStepParameters.class);
    orchestrationElements.put("dummySectionStepTransput", DummySectionStepTransput.class);
    orchestrationElements.put("dummyStepParameters", DummyStepParameters.class);
    orchestrationElements.put("forkStepParameters", ForkStepParameters.class);
    orchestrationElements.put("sectionChainPassThroughData", SectionChainPassThroughData.class);
    orchestrationElements.put("sectionChainStepParameters", SectionChainStepParameters.class);
    orchestrationElements.put("sectionStepParameters", SectionStepParameters.class);
  }
}
