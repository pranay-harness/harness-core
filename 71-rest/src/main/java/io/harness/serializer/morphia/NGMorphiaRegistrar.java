package io.harness.serializer.morphia;

import io.harness.cdng.infra.beans.InfraDefinition;
import io.harness.cdng.infra.beans.InfraMapping;
import io.harness.cdng.infra.beans.K8sDirectInfraDefinition;
import io.harness.cdng.infra.beans.K8sDirectInfraMapping;
import io.harness.morphia.MorphiaRegistrar;

import java.util.Map;
import java.util.Set;

public class NGMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(InfraDefinition.class);
    set.add(InfraMapping.class);
    set.add(K8sDirectInfraDefinition.class);
    set.add(K8sDirectInfraMapping.class);
  }

  @Override
  public void registerImplementationClasses(Map<String, Class> map) {
    // Nothing to register yet
  }
}
