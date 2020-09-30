package io.harness.serializer;

import com.google.common.collect.ImmutableSet;

import io.harness.serializer.kryo.VerificationKryoRegistrar;
import io.harness.serializer.kryo.YamlKryoRegistrar;
import lombok.experimental.UtilityClass;

@UtilityClass
public class VerificationRegistrars {
  public static final ImmutableSet<Class<? extends KryoRegistrar>> kryoRegistrars =
      ImmutableSet.<Class<? extends KryoRegistrar>>builder()
          .add(VerificationKryoRegistrar.class)
          .add(YamlKryoRegistrar.class)
          .build();
}
