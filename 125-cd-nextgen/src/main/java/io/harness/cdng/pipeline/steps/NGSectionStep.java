package io.harness.cdng.pipeline.steps;

import io.harness.executions.steps.ExecutionNodeType;
import io.harness.state.StepType;
import io.harness.steps.section.chain.SectionChainStep;

public class NGSectionStep extends SectionChainStep {
  public static final StepType STEP_TYPE = StepType.builder().type(ExecutionNodeType.GENERIC_SECTION.getName()).build();
}
