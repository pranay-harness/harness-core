package io.harness.serializer.morphia;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrarHelperPut;
import io.harness.steps.approval.step.entities.ApprovalInstance;
import io.harness.steps.approval.step.harness.entities.HarnessApprovalInstance;
import io.harness.steps.approval.step.jira.entities.JiraApprovalInstance;
import io.harness.steps.barriers.BarrierStepParameters;
import io.harness.steps.barriers.beans.BarrierExecutionInstance;
import io.harness.steps.barriers.beans.BarrierOutcome;
import io.harness.steps.cf.FeatureUpdateStepParameters;
import io.harness.steps.dummy.DummySectionOutcome;
import io.harness.steps.dummy.DummySectionStepParameters;
import io.harness.steps.fork.ForkStepParameters;
import io.harness.steps.resourcerestraint.ResourceRestraintStepParameters;
import io.harness.steps.resourcerestraint.beans.ResourceRestraintInstance;
import io.harness.steps.resourcerestraint.beans.ResourceRestraintOutcome;
import io.harness.steps.section.SectionStepParameters;
import io.harness.steps.section.chain.SectionChainPassThroughData;
import io.harness.steps.section.chain.SectionChainStepParameters;

import java.util.Set;

@OwnedBy(HarnessTeam.PIPELINE)
public class OrchestrationStepsMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(BarrierExecutionInstance.class);
    set.add(ResourceRestraintInstance.class);
    set.add(ApprovalInstance.class);
    set.add(HarnessApprovalInstance.class);
    set.add(JiraApprovalInstance.class);
  }

  @Override
  public void registerImplementationClasses(MorphiaRegistrarHelperPut h, MorphiaRegistrarHelperPut w) {
    h.put("steps.barriers.BarrierStepParameters", BarrierStepParameters.class);
    h.put("steps.barriers.beans.BarrierOutcome", BarrierOutcome.class);
    h.put("steps.resourcerestraint.ResourceRestraintStepParameters", ResourceRestraintStepParameters.class);
    h.put("steps.resourcerestraint.beans.ResourceRestraintOutcome", ResourceRestraintOutcome.class);
    h.put("steps.dummy.DummySectionOutcome", DummySectionOutcome.class);
    h.put("steps.dummy.DummySectionStepParameters", DummySectionStepParameters.class);
    h.put("steps.fork.ForkStepParameters", ForkStepParameters.class);
    h.put("steps.section.chain.SectionChainPassThroughData", SectionChainPassThroughData.class);
    h.put("steps.section.chain.SectionStepParameters", SectionChainStepParameters.class);
    h.put("steps.section.SectionStepParameters", SectionStepParameters.class);

    // Feature Flag
    h.put("steps.cf.FeatureUpdateStepParameters", FeatureUpdateStepParameters.class);
  }
}
