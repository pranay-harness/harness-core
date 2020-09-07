package io.harness.walktree.visitor;

import java.util.List;

/**
 * This interface should be implemented by each element which wants
 * its children to traverse.
 */
public interface Visitable {
  /**
   * @return List of objects referring to children on which you want traverse.
   */
  List<Object> getChildrenToWalk();
}
