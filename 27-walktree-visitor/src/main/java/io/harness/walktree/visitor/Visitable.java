package io.harness.walktree.visitor;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.harness.walktree.beans.VisitableChildren;

/**
 * This interface should be implemented by each element which wants
 * its children to traverse.
 */
public interface Visitable {
  /**
   * @return List of objects referring to children on which you want traverse.
   */
  @JsonIgnore VisitableChildren getChildrenToWalk();
}
