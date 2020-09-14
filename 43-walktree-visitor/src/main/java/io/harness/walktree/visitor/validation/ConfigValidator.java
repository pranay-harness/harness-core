package io.harness.walktree.visitor.validation;

import io.harness.walktree.visitor.DummyVisitableElement;

public interface ConfigValidator extends DummyVisitableElement {
  void validate(Object object, ValidationVisitor visitor);
}
