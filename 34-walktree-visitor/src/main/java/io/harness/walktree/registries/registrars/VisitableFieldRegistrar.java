package io.harness.walktree.registries.registrars;

import io.harness.registries.Registrar;
import io.harness.walktree.registries.visitorfield.VisitableFieldProcessor;
import io.harness.walktree.registries.visitorfield.VisitorFieldType;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Set;

public interface VisitableFieldRegistrar extends Registrar<VisitorFieldType, VisitableFieldProcessor<?>> {
  void register(Set<Pair<VisitorFieldType, Class<? extends VisitableFieldProcessor<?>>>> fieldClasses);
}
