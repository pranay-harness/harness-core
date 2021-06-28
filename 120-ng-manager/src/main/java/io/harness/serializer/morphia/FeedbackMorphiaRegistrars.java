package io.harness.serializer.morphia;

import io.harness.morphia.MorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrarHelperPut;
import io.harness.ng.feedback.entities.FeedbackForm;

import java.util.Set;

public class FeedbackMorphiaRegistrars implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(FeedbackForm.class);
  }

  @Override
  public void registerImplementationClasses(MorphiaRegistrarHelperPut h, MorphiaRegistrarHelperPut w) {}
}
