package io.harness.registrars;

import io.harness.beans.ParameterField;
import io.harness.inputset.ParameterVisitorFieldProcessor;
import io.harness.walktree.registries.registrars.VisitableFieldRegistrar;
import io.harness.walktree.registries.visitorfield.VisitableFieldProcessor;
import io.harness.walktree.registries.visitorfield.VisitorFieldType;
import io.harness.walktree.registries.visitorfield.VisitorFieldWrapper;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Set;

public class NGPipelineVisitorFieldRegistrar implements VisitableFieldRegistrar {
  @Override
  public void register(Set<Pair<VisitorFieldType, Class<? extends VisitableFieldProcessor<?>>>> fieldClasses) {
    fieldClasses.add(Pair.of(ParameterField.VISITOR_FIELD_TYPE, ParameterVisitorFieldProcessor.class));
  }

  @Override
  public void registerFieldTypes(Set<Pair<Class<? extends VisitorFieldWrapper>, VisitorFieldType>> fieldTypeClasses) {
    fieldTypeClasses.add(Pair.of(ParameterField.class, ParameterField.VISITOR_FIELD_TYPE));
  }
}
