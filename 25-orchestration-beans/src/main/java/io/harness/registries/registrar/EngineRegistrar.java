package io.harness.registries.registrar;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.reflection.CodeUtils;
import io.harness.registries.RegistrableEntity;
import io.harness.registries.RegistryKey;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@OwnedBy(CDC)
public interface EngineRegistrar<K extends RegistryKey, T extends RegistrableEntity> {
  void register(Set<Pair<K, Class<? extends T>>> registrableEntities);

  default void testClassesModule() {
    final Set<Pair<K, Class<? extends T>>> classes = new HashSet<>();
    register(classes);
    CodeUtils.checkHarnessClassesBelongToModule(
        CodeUtils.location(this.getClass()), classes.stream().map(pair -> pair.getRight()).collect(Collectors.toSet()));
  }
}
