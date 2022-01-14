package io.harness.serializer;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.serializer.morphia.GitSyncMorphiaClassesRegistrar;

import com.google.common.collect.ImmutableSet;

@OwnedBy(DX)
public class GitSyncRegistrars {
  public static final ImmutableSet<Class<? extends MorphiaRegistrar>> morphiaRegistrars =
      ImmutableSet.<Class<? extends MorphiaRegistrar>>builder().add(GitSyncMorphiaClassesRegistrar.class).build();
}
