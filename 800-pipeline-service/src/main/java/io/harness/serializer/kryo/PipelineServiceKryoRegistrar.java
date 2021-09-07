package io.harness.serializer.kryo;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.approval.jira.JiraApprovalCallback;
import io.harness.pms.async.plan.PartialPlanCreatorResponseData;
import io.harness.pms.async.plan.PartialPlanResponseCallback;
import io.harness.serializer.KryoRegistrar;

import com.esotericsoftware.kryo.Kryo;

@OwnedBy(HarnessTeam.PIPELINE)
public class PipelineServiceKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(JiraApprovalCallback.class, 800001);
    kryo.register(PartialPlanResponseCallback.class, 800002);
    kryo.register(PartialPlanCreatorResponseData.class, 800003);
  }
}
