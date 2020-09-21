package io.harness.walktree.visitor;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import io.harness.walktree.visitor.validation.ValidationVisitor;

@Singleton
public class SimpleVisitorFactory {
  @Inject Injector injector;

  public ValidationVisitor obtainValidationVisitor(Class<?> modeType, boolean useFQN) {
    ValidationVisitor validationVisitor = new ValidationVisitor(injector, modeType, useFQN);
    injector.injectMembers(validationVisitor);
    return validationVisitor;
  }
}
