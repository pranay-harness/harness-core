package io.harness.serializer;

import com.google.common.collect.ImmutableSet;

import io.harness.morphia.MorphiaRegistrar;
import io.harness.serializer.kryo.SMCoreKryoRegistrar;
import io.harness.serializer.kryo.SecretManagerClientKryoRegistrar;
import io.harness.serializer.morphia.SMCoreMorphiaRegistrar;
import io.harness.serializer.morphia.SecretManagerClientMorphiaRegistrar;
import lombok.experimental.UtilityClass;

@UtilityClass
public class SMCoreRegistrars {
  public static final ImmutableSet<Class<? extends KryoRegistrar>> kryoRegistrars =
      ImmutableSet.<Class<? extends KryoRegistrar>>builder()
          .add(SecretManagerClientKryoRegistrar.class)
          .add(SMCoreKryoRegistrar.class)
          .build();
  public static final ImmutableSet<Class<? extends MorphiaRegistrar>> morphiaRegistrars =
      ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
          .add(SecretManagerClientMorphiaRegistrar.class)
          .add(SMCoreMorphiaRegistrar.class)
          .build();
}
