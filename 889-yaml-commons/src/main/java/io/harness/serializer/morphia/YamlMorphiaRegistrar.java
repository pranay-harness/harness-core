package io.harness.serializer.morphia;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrarHelperPut;
import io.harness.yaml.core.variables.NumberNGVariable;
import io.harness.yaml.core.variables.StringNGVariable;

import java.util.Set;

@OwnedBy(PIPELINE)
public class YamlMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {}

  @Override
  public void registerImplementationClasses(MorphiaRegistrarHelperPut h, MorphiaRegistrarHelperPut w) {
    h.put("yaml.core.variables.StringNGVariable", StringNGVariable.class);
    h.put("yaml.core.variables.NumberNGVariable", NumberNGVariable.class);
  }
}
