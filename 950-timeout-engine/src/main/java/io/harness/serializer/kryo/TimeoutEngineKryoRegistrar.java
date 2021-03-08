package io.harness.serializer.kryo;

import io.harness.serializer.KryoRegistrar;
import io.harness.timeout.trackers.absolute.AbsoluteTimeoutParameters;

import com.esotericsoftware.kryo.Kryo;

public class TimeoutEngineKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(AbsoluteTimeoutParameters.class, 9501);
  }
}
