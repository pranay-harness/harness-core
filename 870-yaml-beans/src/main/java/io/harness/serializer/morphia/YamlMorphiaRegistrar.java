package io.harness.serializer.morphia;

import io.harness.morphia.MorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrarHelperPut;
import io.harness.yaml.core.intfc.Pipeline;
import io.harness.yaml.core.nonyaml.WithNonYamlInfo;
import io.harness.yaml.core.variables.NumberNGVariable;
import io.harness.yaml.core.variables.StringNGVariable;

import java.util.Set;

public class YamlMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(WithNonYamlInfo.class);
    set.add(Pipeline.class);
  }

  @Override
  public void registerImplementationClasses(MorphiaRegistrarHelperPut h, MorphiaRegistrarHelperPut w) {
    h.put("yaml.core.variables.StringNGVariable", StringNGVariable.class);
    h.put("yaml.core.variables.NumberNGVariable", NumberNGVariable.class);
  }
}
