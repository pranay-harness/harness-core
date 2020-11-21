package io.harness.serializer;

import io.harness.morphia.MorphiaRegistrar;
import io.harness.serializer.kryo.NGTriggerKryoRegistrar;
import io.harness.serializer.morphia.NGTriggerMorphiaRegistrar;
import io.harness.serializer.spring.NGTriggerAliasRegistrar;
import io.harness.spring.AliasRegistrar;

import com.google.common.collect.ImmutableSet;
import lombok.experimental.UtilityClass;

@UtilityClass
public class NGTriggerRegistrars {
  public final ImmutableSet<Class<? extends KryoRegistrar>> kryoRegistrars =
      ImmutableSet.<Class<? extends KryoRegistrar>>builder()
          .addAll(YamlBeansModuleRegistrars.kryoRegistrars)
          .add(NGTriggerKryoRegistrar.class)
          .build();

  public final ImmutableSet<Class<? extends AliasRegistrar>> aliasRegistrars =
      ImmutableSet.<Class<? extends AliasRegistrar>>builder()
          .addAll(YamlBeansModuleRegistrars.aliasRegistrars)
          .add(NGTriggerAliasRegistrar.class)
          .build();

  public final ImmutableSet<Class<? extends MorphiaRegistrar>> morphiaRegistrars =
      ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
          .addAll(YamlBeansModuleRegistrars.morphiaRegistrars)
          .add(NGTriggerMorphiaRegistrar.class)
          .build();
}
