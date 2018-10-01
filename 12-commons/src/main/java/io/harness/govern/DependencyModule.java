package io.harness.govern;

import static com.google.common.collect.Lists.newArrayList;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.inject.AbstractModule;
import com.google.inject.Module;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public abstract class DependencyModule extends AbstractModule {
  public abstract Set<DependencyModule> dependencies();

  public List<Module> cumulativeDependencies() {
    final LinkedList<Module> cumulativeDependencies = new LinkedList<>();
    cumulativeDependencies.push(this);

    final Set<DependencyModule> dependencies = dependencies();
    if (isEmpty(dependencies)) {
      return cumulativeDependencies;
    }

    final Set<Module> initializedModules = new HashSet<>();

    dependencies.forEach(dependency -> {
      if (initializedModules.contains(dependency)) {
        return;
      }

      final List<Module> modules = dependency.cumulativeDependencies();
      if (isNotEmpty(modules)) {
        modules.forEach(module -> {
          if (!initializedModules.contains(module)) {
            cumulativeDependencies.push(module);
            initializedModules.add(module);
          }
        });
      }
    });
    return newArrayList(cumulativeDependencies.descendingIterator());
  }
}
