package io.harness.serializer;

import io.harness.morphia.MorphiaRegistrar;
import io.harness.serializer.kryo.ProjectAndOrgKryoRegistrar;
import io.harness.serializer.morphia.InvitesMorphiaRegistrar;
import io.harness.serializer.morphia.ProjectAndOrgMorphiaRegistrar;
import io.harness.spring.AliasRegistrar;

import com.google.common.collect.ImmutableSet;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ProjectAndOrgRegistrars {
  public final ImmutableSet<Class<? extends KryoRegistrar>> kryoRegistrars =
      ImmutableSet.<Class<? extends KryoRegistrar>>builder()
          .addAll(NGCoreBeansRegistrars.kryoRegistrars)
          .addAll(PersistenceRegistrars.kryoRegistrars)
          .add(ProjectAndOrgKryoRegistrar.class)
          .build();

  public final ImmutableSet<Class<? extends MorphiaRegistrar>> morphiaRegistrars =
      ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
          .addAll(NGCoreBeansRegistrars.morphiaRegistrars)
          .addAll(PersistenceRegistrars.morphiaRegistrars)
          .add(InvitesMorphiaRegistrar.class)
          .add(ProjectAndOrgMorphiaRegistrar.class)
          .build();

  public static final ImmutableSet<Class<? extends AliasRegistrar>> aliasRegistrars =
      ImmutableSet.<Class<? extends AliasRegistrar>>builder()
          .addAll(NGCoreBeansRegistrars.aliasRegistrars)
          .addAll(PersistenceRegistrars.aliasRegistrars)
          .build();
}
