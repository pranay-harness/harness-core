package io.harness.serializer.morphia;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.sso.OauthSettings;
import io.harness.beans.sso.SSOSettings;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrarHelperPut;

import java.util.Set;

@OwnedBy(PL)
public class SSOMorphiaRegistrars implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(SSOSettings.class);
    set.add(OauthSettings.class);
  }

  @Override
  public void registerImplementationClasses(MorphiaRegistrarHelperPut h, MorphiaRegistrarHelperPut w) {}
}
