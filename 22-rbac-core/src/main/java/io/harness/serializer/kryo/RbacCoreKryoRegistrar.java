package io.harness.serializer.kryo;

import com.esotericsoftware.kryo.Kryo;
import io.harness.beans.EnvFilter;
import io.harness.beans.GenericEntityFilter;
import io.harness.beans.UsageRestrictions;
import io.harness.beans.WorkflowFilter;
import io.harness.serializer.KryoRegistrar;

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
