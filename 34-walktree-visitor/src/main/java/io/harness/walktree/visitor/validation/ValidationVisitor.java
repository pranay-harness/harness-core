package io.harness.walktree.visitor.validation;

import com.google.inject.Injector;

import io.harness.walktree.beans.VisitElementResult;
import io.harness.walktree.visitor.SimpleVisitor;

public class ValidationVisitor extends SimpleVisitor<ConfigValidator> {
  public ValidationVisitor(Injector injector) {
    super(injector);
  }

  @Override
  public VisitElementResult visitElement(Object currentElement) {
    ConfigValidator helperClass = getHelperClass(currentElement);
    if (helperClass != null) {
      helperClass.validate(currentElement, this);
    }
    return VisitElementResult.CONTINUE;
  }
}
