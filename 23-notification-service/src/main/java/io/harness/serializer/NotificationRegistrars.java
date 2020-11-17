package io.harness.serializer;

import com.google.common.collect.ImmutableSet;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.serializer.morphia.NotificationMorphiaClassesRegistrar;
import io.harness.spring.AliasRegistrar;

public class NotificationRegistrars {
  public static final ImmutableSet<Class<? extends KryoRegistrar>> kryoRegistrars =
      ImmutableSet.<Class<? extends KryoRegistrar>>builder().build();

  public static final ImmutableSet<Class<? extends MorphiaRegistrar>> morphiaRegistrars =
      ImmutableSet.<Class<? extends MorphiaRegistrar>>builder().add(NotificationMorphiaClassesRegistrar.class).build();

  public static final ImmutableSet<Class<? extends AliasRegistrar>> aliasRegistrars =
      ImmutableSet.<Class<? extends AliasRegistrar>>builder().build();
}
