package io.harness.walktree.visitor;

import com.google.inject.Inject;
import com.google.inject.Injector;

import io.harness.walktree.visitor.validation.ValidationVisitor;

public class SimpleVisitorFactory {
  @Inject Injector injector;

  public ValidationVisitor obtainValidationVisitor() {
    ValidationVisitor validationVisitor = new ValidationVisitor(injector);
    injector.injectMembers(validationVisitor);
    return validationVisitor;
  }
}
