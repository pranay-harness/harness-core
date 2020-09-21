package io.harness.walktree.visitor;

import io.harness.walktree.beans.VisitElementResult;
import io.harness.walktree.beans.VisitableChild;

import java.util.List;
import java.util.stream.Collectors;

public abstract class Visitor {
  /**
   * Invoked for a directory before entries in the Element are visited.
   * @param element
   * @return
   */
  public abstract VisitElementResult preVisitElement(Object element);

  /**
   * Invoked for the current element.
   * @param currentElement
   * @return
   */
  public abstract VisitElementResult visitElement(Object currentElement);

  /**
   * Invoked for an element after children of the element, and all of their
   * descendants, have been visited.
   * @param element
   * @return
   */
  public abstract VisitElementResult postVisitElement(Object element);

  // Entry point to walk the tree
  public VisitElementResult walkElementTree(Object currentElement) {
    // Pre-visit Element
    VisitElementResult prevVisitResult = preVisitElement(currentElement);
    boolean skipToPostVisit = shouldSkipToPostVisit(prevVisitResult);
    if (prevVisitResult == VisitElementResult.TERMINATE) {
      return prevVisitResult;
    }

    // Visit Element
    if (!skipToPostVisit && currentElement instanceof Visitable) {
      VisitElementResult visitElementResult = visitElement(currentElement);
      boolean skipChildren = shouldSkipToPostVisit(visitElementResult);
      if (visitElementResult == VisitElementResult.TERMINATE) {
        return visitElementResult;
      }
      // Visit the children
      if (!skipChildren) {
        Visitable visitable = (Visitable) currentElement;
        if (visitable.getChildrenToWalk() != null) {
          List<Object> childrenToWalk = visitable.getChildrenToWalk()
                                            .getVisitableChildList()
                                            .stream()
                                            .map(VisitableChild::getValue)
                                            .collect(Collectors.toList());
          for (Object child : childrenToWalk) {
            // if child is null then we should not visit it
            if (child == null) {
              continue;
            }
            VisitElementResult childVisitResult = walkElementTree(child);
            if (childVisitResult == VisitElementResult.TERMINATE) {
              return childVisitResult;
            }
            if (childVisitResult == VisitElementResult.SKIP_SIBLINGS) {
              break;
            }
          }
        }
      }
    }

    // Post-visit element
    VisitElementResult postVisitResult = postVisitElement(currentElement);
    if (postVisitResult == VisitElementResult.SKIP_SUBTREE) {
      return VisitElementResult.CONTINUE;
    } else {
      return postVisitResult;
    }
  }

  public boolean shouldSkipToPostVisit(VisitElementResult result) {
    return result == VisitElementResult.SKIP_SIBLINGS || result == VisitElementResult.SKIP_SUBTREE;
  }
}
