package io.harness.accesscontrol.serializer;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.accesscontrol.serializer.kryo.AccessControlClientKryoRegistrar;
import io.harness.annotations.dev.OwnedBy;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.serializer.KryoRegistrar;

import com.google.common.collect.ImmutableSet;
import lombok.experimental.UtilityClass;

@OwnedBy(PL)
@UtilityClass
public class AccessControlClientRegistrars {
  public static final ImmutableSet<Class<? extends KryoRegistrar>> kryoRegistrars =
      ImmutableSet.<Class<? extends KryoRegistrar>>builder().add(AccessControlClientKryoRegistrar.class).build();
  public static final ImmutableSet<Class<? extends MorphiaRegistrar>> morphiaRegistrars =
      ImmutableSet.<Class<? extends MorphiaRegistrar>>builder().build();
}
