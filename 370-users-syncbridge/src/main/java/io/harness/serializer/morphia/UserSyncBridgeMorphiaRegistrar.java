package io.harness.serializer.morphia;

import io.harness.core.userchangestream.UserMembershipChangeStreamState;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrarHelperPut;

import java.util.Set;

public class UserSyncBridgeMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(UserMembershipChangeStreamState.class);
  }

  @Override
  public void registerImplementationClasses(MorphiaRegistrarHelperPut h, MorphiaRegistrarHelperPut w) {
    // No class to register
  }
}