package io.harness.serializer;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.serializer.kryo.SSOKryoRegistrar;
import io.harness.serializer.morphia.SSOMorphiaRegistrars;

import com.google.common.collect.ImmutableSet;
import lombok.experimental.UtilityClass;
import org.mongodb.morphia.converters.TypeConverter;

@UtilityClass
@OwnedBy(PL)
public class AuthenticationManagerRegistrars {
  public static final ImmutableSet<Class<? extends MorphiaRegistrar>> morphiaRegistrars =
      ImmutableSet.<Class<? extends MorphiaRegistrar>>builder().add(SSOMorphiaRegistrars.class).build();

  public static final ImmutableSet<Class<? extends TypeConverter>> morphiaConverters =
      ImmutableSet.<Class<? extends TypeConverter>>builder().build();

  public static final ImmutableSet<Class<? extends KryoRegistrar>> kryoRegistrars =
      ImmutableSet.<Class<? extends KryoRegistrar>>builder().add(SSOKryoRegistrar.class).build();
}
