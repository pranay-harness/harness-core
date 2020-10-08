package io.harness.serializer;

import com.google.common.collect.ImmutableSet;

import io.harness.morphia.MorphiaRegistrar;
import io.harness.serializer.morphia.ConnectorMorphiaClassesRegistrar;
import io.harness.serializer.morphia.InvitesMorphiaRegistrar;
import io.harness.serializer.morphia.ProjectAndOrgMorphiaRegistrar;
import io.harness.serializer.morphia.SecretManagerClientMorphiaRegistrar;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ConnectorNextGenRegistrars {
  public static final ImmutableSet<Class<? extends KryoRegistrar>> kryoRegistrars =
      ImmutableSet.<Class<? extends KryoRegistrar>>builder().build();

  public static final ImmutableSet<Class<? extends MorphiaRegistrar>> morphiaRegistrars =
      ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
          .addAll(OrchestrationBeansRegistrars.morphiaRegistrars)
          .addAll(DelegateServiceDriverRegistrars.morphiaRegistrars)
          .addAll(NGCoreRegistrars.morphiaRegistrars)
          .add(SecretManagerClientMorphiaRegistrar.class)
          .add(ConnectorMorphiaClassesRegistrar.class)
          .add(ProjectAndOrgMorphiaRegistrar.class)
          .add(InvitesMorphiaRegistrar.class)
          .build();
}
