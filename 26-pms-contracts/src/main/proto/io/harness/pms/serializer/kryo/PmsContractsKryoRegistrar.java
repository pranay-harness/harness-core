package io.harness.pms.serializer.kryo;

import com.esotericsoftware.kryo.Kryo;
import io.harness.pms.ambiance.Ambiance;
import io.harness.pms.ambiance.Level;
import io.harness.pms.execution.ExecutionMode;
import io.harness.serializer.KryoRegistrar;

public class PmsContractsKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(Ambiance.class, 2601);
    kryo.register(Level.class, 2602);
    kryo.register(ExecutionMode.class, 2603);
  }
}