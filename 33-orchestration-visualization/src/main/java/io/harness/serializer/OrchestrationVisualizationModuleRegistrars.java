package io.harness.serializer;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.google.common.collect.ImmutableSet;

import io.harness.annotations.dev.OwnedBy;
import io.harness.serializer.kryo.OrchestrationVisualizationKryoRegistrar;
import lombok.experimental.UtilityClass;

@OwnedBy(CDC)
@UtilityClass
public class OrchestrationVisualizationModuleRegistrars {
  public final ImmutableSet<Class<? extends KryoRegistrar>> kryoRegistrars =
      ImmutableSet.<Class<? extends KryoRegistrar>>builder()
          .addAll(OrchestrationRegistrars.kryoRegistrars)
          .add(OrchestrationVisualizationKryoRegistrar.class)
          .build();
}
