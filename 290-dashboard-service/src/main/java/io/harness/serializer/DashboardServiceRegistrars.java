package io.harness.serializer;

import com.google.common.collect.ImmutableSet;

public class DashboardServiceRegistrars {
  public static final ImmutableSet<Class<? extends KryoRegistrar>> kryoRegistrars =
      ImmutableSet.<Class<? extends KryoRegistrar>>builder().addAll(NGCoreClientRegistrars.kryoRegistrars).build();
}
