package io.harness.serializer;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.google.common.collect.ImmutableSet;

import io.harness.annotations.dev.OwnedBy;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.serializer.kryo.PmsSdkKryoRegistrar;
import io.harness.spring.AliasRegistrar;
import lombok.experimental.UtilityClass;
import org.mongodb.morphia.converters.TypeConverter;

@OwnedBy(CDC)
@UtilityClass
public class PmsSdkModuleRegistrars {
  public final ImmutableSet<Class<? extends KryoRegistrar>> kryoRegistrars =
      ImmutableSet.<Class<? extends KryoRegistrar>>builder()
          .addAll(OrchestrationRegistrars.kryoRegistrars)
          .add(PmsSdkKryoRegistrar.class)
          .build();

  public final ImmutableSet<Class<? extends MorphiaRegistrar>> morphiaRegistrars =
      ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
          .addAll(OrchestrationRegistrars.morphiaRegistrars)
          .build();

  public final ImmutableSet<Class<? extends AliasRegistrar>> aliasRegistrars =
      ImmutableSet.<Class<? extends AliasRegistrar>>builder().addAll(OrchestrationRegistrars.aliasRegistrars).build();

  public final ImmutableSet<Class<? extends TypeConverter>> morphiaConverters =
      ImmutableSet.<Class<? extends TypeConverter>>builder().addAll(OrchestrationRegistrars.morphiaConverters).build();
}
