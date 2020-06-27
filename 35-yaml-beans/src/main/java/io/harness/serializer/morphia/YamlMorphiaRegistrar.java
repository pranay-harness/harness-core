package io.harness.serializer.morphia;

import io.harness.morphia.MorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrarHelperPut;
import io.harness.yaml.core.intfc.Pipeline;
import io.harness.yaml.core.intfc.WithIdentifier;
import io.harness.yaml.core.nonyaml.WithNonYamlInfo;

import java.util.Set;

public class YamlMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(WithNonYamlInfo.class);
    set.add(WithIdentifier.class);
    set.add(Pipeline.class);
  }

  @Override
  public void registerImplementationClasses(MorphiaRegistrarHelperPut h, MorphiaRegistrarHelperPut w) {
    // nothing to registrer
  }
}
