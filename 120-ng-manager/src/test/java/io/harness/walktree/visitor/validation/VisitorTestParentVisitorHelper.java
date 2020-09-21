package io.harness.walktree.visitor.validation;

public class VisitorTestParentVisitorHelper implements ConfigValidator {
  @Override
  public void validate(Object object, ValidationVisitor visitor) {}

  @Override
  public Object createDummyVisitableElement(Object originalElement) {
    return VisitorTestParent.builder().build();
  }
}
