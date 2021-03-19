package io.harness.serializer;

import io.harness.morphia.MorphiaRegistrar;
import io.harness.serializer.morphia.GitSyncMorphiaClassesRegistrar;

import com.google.common.collect.ImmutableSet;

public class GitSyncRegistrars {
  public static final ImmutableSet<Class<? extends MorphiaRegistrar>> morphiaRegistrars =
      ImmutableSet.<Class<? extends MorphiaRegistrar>>builder().add(GitSyncMorphiaClassesRegistrar.class).build();
}
