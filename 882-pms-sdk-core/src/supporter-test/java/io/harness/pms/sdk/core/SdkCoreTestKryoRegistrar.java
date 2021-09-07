package io.harness.pms.sdk.core;

import io.harness.pms.sdk.core.execution.events.node.resume.DummyErrorResponseData;
import io.harness.serializer.KryoRegistrar;

import com.esotericsoftware.kryo.Kryo;

public class SdkCoreTestKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(DummyErrorResponseData.class, 123222);
  }
}
