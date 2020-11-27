package io.harness.serializer.kryo;

import io.harness.serializer.KryoRegistrar;

import software.wings.security.EnvFilter;
import software.wings.security.GenericEntityFilter;
import software.wings.security.UsageRestrictions;
import software.wings.security.WorkflowFilter;

import com.esotericsoftware.kryo.Kryo;

public class RbacCoreKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(UsageRestrictions.class, 5247);
    kryo.register(UsageRestrictions.AppEnvRestriction.class, 5248);
    kryo.register(GenericEntityFilter.class, 5249);
    kryo.register(EnvFilter.class, 5250);
    kryo.register(WorkflowFilter.class, 5251);
  }
}
