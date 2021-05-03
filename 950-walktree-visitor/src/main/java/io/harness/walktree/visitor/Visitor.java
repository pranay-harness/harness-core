package io.harness.walktree.visitor;

import io.harness.walktree.beans.VisitElementResult;
import io.harness.walktree.beans.VisitableChild;
import io.harness.walktree.visitor.utilities.VisitorParentPathUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.Getter;

public abstract class Visitor {
  @Getter Map<String, Object> contextMap = new ConcurrentHashMap<>();

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
          Map<String, Object> childrenToWalk =
              visitable.getChildrenToWalk()
                  .getVisitableChildList()
                  .stream()
                  .filter(visitableChild -> visitableChild.getValue() != null)
                  .collect(Collectors.toMap(VisitableChild::getFieldName, VisitableChild::getValue));
          for (Map.Entry<String, Object> fieldNameToChild : childrenToWalk.entrySet()) {
            Object child = fieldNameToChild.getValue();
            // if child is null then we should not visit it
            if (child == null) {
              continue;
            }
            VisitorParentPathUtils.addToParentList(this.getContextMap(), fieldNameToChild.getKey());
            VisitElementResult childVisitResult = walkElementTree(child);
            VisitorParentPathUtils.removeFromParentList(this.getContextMap());
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
