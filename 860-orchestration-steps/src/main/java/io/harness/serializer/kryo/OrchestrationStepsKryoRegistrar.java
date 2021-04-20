package io.harness.serializer.kryo;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.plancreator.steps.http.HttpStepInfo;
import io.harness.serializer.KryoRegistrar;
import io.harness.steps.approval.stage.ApprovalStageStepParameters;
import io.harness.steps.approval.step.harness.HarnessApprovalOutcome;
import io.harness.steps.approval.step.harness.HarnessApprovalResponseData;
import io.harness.steps.approval.step.jira.JiraApprovalOutcome;
import io.harness.steps.approval.step.jira.beans.JiraApprovalResponseData;
import io.harness.steps.barriers.BarrierStepParameters;
import io.harness.steps.barriers.beans.BarrierExecutionInstance;
import io.harness.steps.barriers.beans.BarrierOutcome;
import io.harness.steps.barriers.beans.BarrierResponseData;
import io.harness.steps.dummy.DummySectionOutcome;
import io.harness.steps.dummy.DummySectionStepParameters;
import io.harness.steps.dummy.DummySectionStepTransput;
import io.harness.steps.fork.ForkStepParameters;
import io.harness.steps.http.HttpOutcome;
import io.harness.steps.jira.JiraIssueOutcome;
import io.harness.steps.resourcerestraint.ResourceRestraintStepParameters;
import io.harness.steps.resourcerestraint.beans.AcquireMode;
import io.harness.steps.resourcerestraint.beans.HoldingScope;
import io.harness.steps.resourcerestraint.beans.ResourceRestraintInstance;
import io.harness.steps.resourcerestraint.beans.ResourceRestraintOutcome;
import io.harness.steps.resourcerestraint.beans.ResourceRestraintResponseData;
import io.harness.steps.section.SectionStepParameters;
import io.harness.steps.section.chain.SectionChainPassThroughData;
import io.harness.steps.section.chain.SectionChainStepParameters;

import com.esotericsoftware.kryo.Kryo;

@OwnedBy(PIPELINE)
public class OrchestrationStepsKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(BarrierExecutionInstance.class, 3201);
    kryo.register(BarrierResponseData.class, 3202);
    kryo.register(BarrierOutcome.class, 3203);
    kryo.register(BarrierStepParameters.class, 3204);
    kryo.register(ResourceRestraintInstance.class, 3205);
    kryo.register(ResourceRestraintStepParameters.class, 3206);
    kryo.register(ResourceRestraintOutcome.class, 3207);
    kryo.register(AcquireMode.class, 3208);
    kryo.register(ResourceRestraintResponseData.class, 3209);
    kryo.register(HoldingScope.class, 3210);

    kryo.register(ForkStepParameters.class, 3211);
    kryo.register(SectionStepParameters.class, 3212);
    kryo.register(DummySectionStepParameters.class, 3213);
    kryo.register(SectionChainStepParameters.class, 3214);

    kryo.register(DummySectionStepTransput.class, 3215);
    kryo.register(DummySectionOutcome.class, 3216);

    kryo.register(SectionChainPassThroughData.class, 3217);

    kryo.register(ApprovalStageStepParameters.class, 3218);
    kryo.register(HarnessApprovalResponseData.class, 3220);
    kryo.register(HarnessApprovalOutcome.class, 3221);
    kryo.register(JiraApprovalResponseData.class, 3223);
    kryo.register(JiraApprovalOutcome.class, 3224);
    kryo.register(JiraIssueOutcome.class, 3225);

    // made it same as which was in CD
    kryo.register(HttpStepInfo.class, 8048);
    kryo.register(HttpOutcome.class, 12501);
  }
}
