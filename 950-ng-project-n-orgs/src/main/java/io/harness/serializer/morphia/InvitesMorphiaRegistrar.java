package io.harness.serializer.morphia;

import io.harness.morphia.MorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrarHelperPut;
import io.harness.ng.core.invites.entities.Invite;
import io.harness.ng.core.invites.entities.UserProjectMap;
import io.harness.ng.core.invites.ext.mail.EmailData;

import java.util.Set;

public class InvitesMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(Invite.class);
    set.add(UserProjectMap.class);
    set.add(EmailData.class);
  }

  @Override
  public void registerImplementationClasses(MorphiaRegistrarHelperPut h, MorphiaRegistrarHelperPut w) {
    // Nothing to register
  }
}
