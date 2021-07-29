package io.harness.serializer.kryo;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.data.OrchestrationMap;
import io.harness.pms.data.PmsOutcome;
import io.harness.pms.execution.facilitator.DefaultFacilitatorParams;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.validation.InputSetValidator;
import io.harness.pms.yaml.validation.InputSetValidatorType;
import io.harness.serializer.KryoRegistrar;

import com.esotericsoftware.kryo.Kryo;

@OwnedBy(HarnessTeam.PIPELINE)
public class PmsCommonsKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    // keeping ids same
    kryo.register(ParameterField.class, 35001);
    kryo.register(InputSetValidator.class, 35002);
    kryo.register(InputSetValidatorType.class, 35008);
    kryo.register(DefaultFacilitatorParams.class, 2515);

    kryo.register(OrchestrationMap.class, 88401);
    kryo.register(PmsOutcome.class, 88402);
  }
}
