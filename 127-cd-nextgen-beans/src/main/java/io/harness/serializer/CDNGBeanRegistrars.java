package io.harness.serializer;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.serializer.kryo.NGBeanKryoRegistrar;
import io.harness.serializer.morphia.NGBeanMorphiaRegistrar;
import io.harness.yaml.schema.beans.YamlSchemaRootClass;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.CDP)
@UtilityClass
public class CDNGBeanRegistrars {
  public final ImmutableSet<Class<? extends KryoRegistrar>> kryoRegistrars =
      ImmutableSet.<Class<? extends KryoRegistrar>>builder()
          .addAll(ManagerRegistrars.kryoRegistrars)
          .addAll(OrchestrationStepsModuleRegistrars.kryoRegistrars)
          .addAll(NGPipelineRegistrars.kryoRegistrars)
          .add(NGBeanKryoRegistrar.class)
          .build();

  public final ImmutableSet<Class<? extends MorphiaRegistrar>> morphiaRegistrars =
      ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
          .addAll(ManagerRegistrars.morphiaRegistrars)
          .addAll(OrchestrationStepsModuleRegistrars.morphiaRegistrars)
          .addAll(NGPipelineRegistrars.morphiaRegistrars)
          .add(NGBeanMorphiaRegistrar.class)
          .build();

  public static final ImmutableList<YamlSchemaRootClass> yamlSchemaRegistrars =
      ImmutableList.<YamlSchemaRootClass>builder().build();
}
