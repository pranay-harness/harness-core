package io.harness.walktree.visitor;

import io.harness.walktree.beans.VisitableChildren;

/**
 * This interface is implemented by VisitorHelper to give dummy visitable element.
 */
public interface DummyVisitableElement {
  Object createDummyVisitableElement(Object originalElement);

  /**
   * Used to handle objects that cannot be handled by the Framework i.e Collection objects and Map.
   * @param object
   * @param visitor
   * @param visitableChildren
   */
  default void handleComplexVisitableChildren(
      Object object, SimpleVisitor visitor, VisitableChildren visitableChildren) {
    // Override this if you have list or map objects in your class.
  }
}
