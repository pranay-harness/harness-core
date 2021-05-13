package io.harness.serializer.morphia;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrarHelperPut;
import io.harness.ng.core.invites.entities.Invite;
import io.harness.ng.core.user.entities.UserProjectMap;
import io.harness.ng.core.user.remote.dto.UserMembership;

import java.util.Set;

@OwnedBy(PL)
public class InvitesMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(Invite.class);
    set.add(UserMembership.class);
    set.add(UserProjectMap.class);
  }

  @Override
  public void registerImplementationClasses(MorphiaRegistrarHelperPut h, MorphiaRegistrarHelperPut w) {
    // Nothing to register
  }
}
