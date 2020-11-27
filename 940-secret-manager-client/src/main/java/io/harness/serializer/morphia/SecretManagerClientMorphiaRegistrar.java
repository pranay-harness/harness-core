package io.harness.serializer.morphia;

import io.harness.morphia.MorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrarHelperPut;
import io.harness.ng.core.models.Secret;
import io.harness.secretmanagerclient.dto.NGSecretManagerConfigDTOConverter;

import java.util.Set;

public class SecretManagerClientMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(NGSecretManagerConfigDTOConverter.class);
    set.add(Secret.class);
  }

  @Override
  public void registerImplementationClasses(MorphiaRegistrarHelperPut h, MorphiaRegistrarHelperPut w) {}
}
