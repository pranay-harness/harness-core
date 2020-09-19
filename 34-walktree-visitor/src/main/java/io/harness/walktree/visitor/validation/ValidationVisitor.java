package io.harness.walktree.visitor.validation;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import io.harness.walktree.beans.VisitElementResult;
import io.harness.walktree.visitor.SimpleVisitor;

@Singleton
public class ValidationVisitor extends SimpleVisitor<ConfigValidator> {
  @Inject
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
