package io.harness.registrars;

import com.google.inject.Inject;
import com.google.inject.Injector;

import io.harness.beans.ParameterField;
import io.harness.ngpipeline.inputset.ParameterVisitorFieldProcessor;
import io.harness.walktree.registries.registrars.VisitableFieldRegistrar;
import io.harness.walktree.registries.visitorfield.VisitableFieldProcessor;
import io.harness.walktree.registries.visitorfield.VisitorFieldType;
import io.harness.walktree.registries.visitorfield.VisitorFieldWrapper;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Set;

public class NGPipelineVisitorFieldRegistrar implements VisitableFieldRegistrar {
  @Inject private Injector injector;

  @Override
  public void register(Set<Pair<VisitorFieldType, VisitableFieldProcessor<?>>> fieldClasses) {
    fieldClasses.add(
        Pair.of(ParameterField.VISITOR_FIELD_TYPE, injector.getInstance(ParameterVisitorFieldProcessor.class)));
  }

  @Override
  public void registerFieldTypes(Set<Pair<Class<? extends VisitorFieldWrapper>, VisitorFieldType>> fieldTypeClasses) {
    fieldTypeClasses.add(Pair.of(ParameterField.class, ParameterField.VISITOR_FIELD_TYPE));
  }
}
