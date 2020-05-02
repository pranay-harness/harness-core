package io.harness.registries.registrar;

import io.harness.reflection.CodeUtils;
import io.harness.registries.RegistrableEntity;

import java.util.HashSet;
import java.util.Set;

public interface EngineRegistrar<T extends RegistrableEntity> {
  void register(Set<Class<? extends T>> registrableEntities);

  default void testClassesModule() {
    final Set<Class<? extends T>> classes = new HashSet<>();
    register(classes);
    CodeUtils.checkHarnessClassesBelongToModule(CodeUtils.location(this.getClass()), classes);
  }
}
