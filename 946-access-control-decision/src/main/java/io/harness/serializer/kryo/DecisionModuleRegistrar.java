package io.harness.serializer.kryo;

import io.harness.accesscontrol.clients.AccessCheckRequestDTO;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.serializer.KryoRegistrar;

import com.esotericsoftware.kryo.Kryo;

@OwnedBy(HarnessTeam.PL)
public class DecisionModuleRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(AccessCheckRequestDTO.class, 112350);
  }
}
