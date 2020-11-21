package io.harness.walktree.registries.registrars;

import io.harness.registries.Registrar;
import io.harness.walktree.registries.visitorfield.VisitableFieldProcessor;
import io.harness.walktree.registries.visitorfield.VisitorFieldType;
import io.harness.walktree.registries.visitorfield.VisitorFieldWrapper;

import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;

public interface VisitableFieldRegistrar extends Registrar<VisitorFieldType, VisitableFieldProcessor<?>> {
  void register(Set<Pair<VisitorFieldType, VisitableFieldProcessor<?>>> fieldClasses);
  void registerFieldTypes(Set<Pair<Class<? extends VisitorFieldWrapper>, VisitorFieldType>> fieldTypeClasses);
}
