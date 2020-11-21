package io.harness.registries;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.reflection.CodeUtils;

import com.google.inject.Injector;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;

@OwnedBy(CDC)
public interface Registrar<K, T> {
  void register(Set<Pair<K, T>> registrableEntities);

  default void testClassesModule() {
    final Set<Pair<K, T>> classes = new HashSet<>();
    register(classes);
    CodeUtils.checkHarnessClassesBelongToModule(CodeUtils.location(this.getClass()),
        classes.stream().map(pair -> pair.getRight().getClass()).collect(Collectors.toSet()));
  }
}
