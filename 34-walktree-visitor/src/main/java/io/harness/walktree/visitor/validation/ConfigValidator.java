package io.harness.walktree.visitor.validation;

import io.harness.walktree.beans.VisitableChildren;
import io.harness.walktree.visitor.DummyVisitableElement;

public interface ConfigValidator extends DummyVisitableElement {
  /**
   * used to do object specific validation.
   *
   * The error is stored in visitor.errorMap as uuid -> ValidationErrors
   * The uuid should be present in one of the field.
   *
   * @param object
   * @param visitor
   */
  void validate(Object object, ValidationVisitor visitor);

  /**
   * Used to handle objects that cannot be handled by the Validation Framework i.e Collection objects and Map.
   * @param object
   * @param visitor
   * @param visitableChildren
   */
  default void handleComplexVisitableChildren(
      Object object, ValidationVisitor visitor, VisitableChildren visitableChildren) {
    // Override this if you have list or map objects in your class.
  }
}
