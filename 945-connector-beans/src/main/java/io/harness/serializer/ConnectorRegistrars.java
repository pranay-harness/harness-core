package io.harness.serializer;

import io.harness.serializer.kryo.ConnectorBeansKryoRegistrar;

import com.google.common.collect.ImmutableSet;

public class ConnectorRegistrars {
  public static final ImmutableSet<Class<? extends KryoRegistrar>> kryoRegistrars =
      ImmutableSet.<Class<? extends KryoRegistrar>>builder().add(ConnectorBeansKryoRegistrar.class).build();
}
