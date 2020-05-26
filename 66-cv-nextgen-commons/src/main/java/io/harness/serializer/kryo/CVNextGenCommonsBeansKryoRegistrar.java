package io.harness.serializer.kryo;

import static io.harness.annotations.dev.HarnessTeam.CV;

import com.esotericsoftware.kryo.Kryo;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.beans.CVHistogram;
import io.harness.serializer.KryoRegistrar;

@OwnedBy(CV)
public class CVNextGenCommonsBeansKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) throws Exception {
    kryo.register(CVHistogram.class, 7382);
    kryo.register(CVHistogram.Bar.class, 7383);
  }
}