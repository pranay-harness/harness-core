package io.harness.connector;

import com.google.common.collect.ImmutableSet;

import io.harness.serializer.KryoRegistrar;

public class ConnectorRegistrar {
  public static final ImmutableSet<Class<? extends KryoRegistrar>> kryoRegistrars =
      ImmutableSet.<Class<? extends KryoRegistrar>>builder().build();
}
