package io.harness.serializer.kryo;

import com.esotericsoftware.kryo.Kryo;
import io.harness.beans.sweepingoutputs.ContextElement;
import io.harness.beans.sweepingoutputs.K8PodDetails;
import io.harness.serializer.KryoRegistrar;

/**
 * Class will register all kryo classes
 */

public class CIBeansRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) throws Exception {
    kryo.register(K8PodDetails.class, 100001);
    kryo.register(ContextElement.class, 100002);
  }
}
