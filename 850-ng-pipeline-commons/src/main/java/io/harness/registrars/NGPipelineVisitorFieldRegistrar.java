package io.harness.registrars;

import io.harness.annotations.dev.ToBeDeleted;
import io.harness.ngpipeline.inputset.ParameterVisitorFieldProcessor;
import io.harness.pms.yaml.ParameterField;
import io.harness.walktree.registries.registrars.VisitableFieldRegistrar;
import io.harness.walktree.registries.visitorfield.VisitableFieldProcessor;
import io.harness.walktree.registries.visitorfield.VisitorFieldType;
import io.harness.walktree.registries.visitorfield.VisitorFieldWrapper;

import com.google.inject.Inject;
import com.google.inject.Injector;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;

@ToBeDeleted
@Deprecated
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
