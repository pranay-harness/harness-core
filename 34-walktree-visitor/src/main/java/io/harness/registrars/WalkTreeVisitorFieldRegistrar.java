package io.harness.registrars;

import io.harness.walktree.beans.DummyVisitorField;
import io.harness.walktree.beans.DummyVisitorFieldProcessor;
import io.harness.walktree.registries.registrars.VisitableFieldRegistrar;
import io.harness.walktree.registries.visitorfield.VisitableFieldProcessor;
import io.harness.walktree.registries.visitorfield.VisitorFieldType;
import io.harness.walktree.registries.visitorfield.VisitorFieldWrapper;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Set;

public class WalkTreeVisitorFieldRegistrar implements VisitableFieldRegistrar {
  @Override
  public void register(Set<Pair<VisitorFieldType, Class<? extends VisitableFieldProcessor<?>>>> fieldClasses) {
    fieldClasses.add(Pair.of(DummyVisitorField.VISITOR_FIELD_TYPE, DummyVisitorFieldProcessor.class));
  }

  @Override
  public void registerFieldTypes(Set<Pair<Class<? extends VisitorFieldWrapper>, VisitorFieldType>> fieldTypeClasses) {
    fieldTypeClasses.add(Pair.of(DummyVisitorField.class, DummyVisitorField.VISITOR_FIELD_TYPE));
  }
}
