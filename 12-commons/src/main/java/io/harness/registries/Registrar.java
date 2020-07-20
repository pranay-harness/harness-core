package io.harness.registries;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.reflection.CodeUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@OwnedBy(CDC)
public interface Registrar<K extends RegistryKey, T extends RegistrableEntity> {
  void register(Set<Pair<K, Class<? extends T>>> registrableEntities);

  default void testClassesModule() {
    final Set<Pair<K, Class<? extends T>>> classes = new HashSet<>();
    register(classes);
    CodeUtils.checkHarnessClassesBelongToModule(
        CodeUtils.location(this.getClass()), classes.stream().map(Pair::getRight).collect(Collectors.toSet()));
  }
}
