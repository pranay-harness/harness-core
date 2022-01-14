package io.harness.accesscontrol.serializer.kryo;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.accesscontrol.AccessDeniedErrorDTO;
import io.harness.accesscontrol.NGAccessDeniedException;
import io.harness.accesscontrol.clients.PermissionCheckDTO;
import io.harness.accesscontrol.clients.ResourceScope;
import io.harness.annotations.dev.OwnedBy;
import io.harness.serializer.KryoRegistrar;

import com.esotericsoftware.kryo.Kryo;

@OwnedBy(PL)
public class AccessControlClientKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(AccessDeniedErrorDTO.class, 70001);
    kryo.register(PermissionCheckDTO.class, 70002);
    kryo.register(ResourceScope.class, 70003);
    kryo.register(NGAccessDeniedException.class, 70004);
  }
}
