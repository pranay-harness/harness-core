package io.harness.cdng.pipeline.steps;

import io.harness.executions.steps.ExecutionNodeType;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.steps.section.chain.SectionChainStep;

public class NGSectionStep extends SectionChainStep {
  public static final StepType STEP_TYPE = StepType.newBuilder()
                                               .setType(ExecutionNodeType.GENERIC_SECTION.getName())
                                               .setStepCategory(StepCategory.STEP)
                                               .build();
}
