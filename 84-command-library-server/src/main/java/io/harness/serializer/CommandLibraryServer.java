package io.harness.serializer;

import com.google.common.collect.ImmutableSet;

import io.harness.morphia.MorphiaRegistrar;
import io.harness.serializer.morphia.CommandLibraryServerMorphiaRegistrar;
import lombok.experimental.UtilityClass;

@UtilityClass
public class CommandLibraryServer {
  public static final com.google.common.collect.ImmutableSet<Class<? extends MorphiaRegistrar>> morphiaRegistrars =
      ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
          .addAll(CommonsRegistrars.morphiaRegistrars)
          .add(CommandLibraryServerMorphiaRegistrar.class)
          .build();
}
