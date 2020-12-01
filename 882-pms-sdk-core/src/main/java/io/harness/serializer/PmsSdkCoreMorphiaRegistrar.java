package io.harness.serializer;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrarHelperPut;
import io.harness.pms.sdk.core.data.Outcome;
import io.harness.pms.sdk.core.data.StepTransput;
import io.harness.pms.sdk.core.data.SweepingOutput;

import java.util.Set;

@OwnedBy(CDC)
public class PmsSdkCoreMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(Outcome.class);
    set.add(SweepingOutput.class);
    set.add(StepTransput.class);
  }

  @Override
  public void registerImplementationClasses(MorphiaRegistrarHelperPut h, MorphiaRegistrarHelperPut w) {
    // Nothing to register
  }
}
