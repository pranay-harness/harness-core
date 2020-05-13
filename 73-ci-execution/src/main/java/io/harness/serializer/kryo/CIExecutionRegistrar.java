package io.harness.serializer.kryo;

import com.esotericsoftware.kryo.Kryo;
import io.harness.serializer.KryoRegistrar;

/**
 * Class will register all kryo classes
 */

public class CIExecutionRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) throws Exception {
    // No class to register
  }
}