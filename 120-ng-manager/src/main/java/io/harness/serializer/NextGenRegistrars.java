package io.harness.serializer;

import io.harness.morphia.MorphiaRegistrar;
import io.harness.serializer.morphia.UserGroupMorphiaRegistrar;

import com.google.common.collect.ImmutableSet;

public class NextGenRegistrars {
  public static final ImmutableSet<Class<? extends KryoRegistrar>> kryoRegistrars =
      ImmutableSet.<Class<? extends KryoRegistrar>>builder()
          .addAll(SecretManagerClientRegistrars.kryoRegistrars)
          .addAll(ConnectorNextGenRegistrars.kryoRegistrars)
          .addAll(CDNGRegistrars.kryoRegistrars)
          .build();

  public static final ImmutableSet<Class<? extends MorphiaRegistrar>> morphiaRegistrars =
      ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
          .addAll(SecretManagerClientRegistrars.morphiaRegistrars)
          .addAll(ConnectorNextGenRegistrars.morphiaRegistrars)
          .addAll(CDNGRegistrars.morphiaRegistrars)
          .add(UserGroupMorphiaRegistrar.class)
          .build();
}
