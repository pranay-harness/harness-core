package io.harness.serializer.kryo;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.ExecutionInterruptType;
import io.harness.beans.SweepingOutput;
import io.harness.context.ContextElementType;
import io.harness.serializer.KryoRegistrar;

import software.wings.api.ExecutionDataValue;
import software.wings.beans.CountsByStatuses;
import software.wings.beans.EntityType;
import software.wings.beans.ErrorStrategy;
import software.wings.beans.ExecutionStrategy;
import software.wings.beans.GitFileConfig;
import software.wings.beans.LicenseInfo;
import software.wings.beans.PhaseStepType;
import software.wings.beans.VariableType;
import software.wings.beans.trigger.WebhookSource;
import software.wings.sm.ExecutionInterruptEffect;
import software.wings.sm.PipelineSummary;
import software.wings.sm.StateTypeScope;

import com.esotericsoftware.kryo.Kryo;

@OwnedBy(CDP)
public class CgOrchestrationBeansKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(ContextElementType.class, 4004);
    kryo.register(GitFileConfig.class, 5472);
    kryo.register(LicenseInfo.class, 5511);

    // Put promoted classes here and do not change the id
    kryo.register(SweepingOutput.class, 3101);
    kryo.register(ExecutionInterruptType.class, 4000);
    kryo.register(ExecutionDataValue.class, 5368);
    kryo.register(CountsByStatuses.class, 4008);
    kryo.register(EntityType.class, 5360);
    kryo.register(ErrorStrategy.class, 4005);
    kryo.register(ExecutionStrategy.class, 4002);
    kryo.register(PhaseStepType.class, 5026);
    kryo.register(VariableType.class, 5379);
    kryo.register(ExecutionInterruptEffect.class, 5236);
    kryo.register(PipelineSummary.class, 5142);
    kryo.register(StateTypeScope.class, 5144);
    kryo.register(WebhookSource.class, 8551);
  }
}
