/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.walktree.beans;

public enum VisitElementResult {
  /**
   * Continue. When returned from a preVisit
   * method then the entries in the directory should also be visited.
   */
  CONTINUE,
  /**
   * Terminate. PostVisit will also not be invoked.
   */
  TERMINATE,
  /**
   * Continue without visiting the children. If returned from preVisitElement,
   * then current element is also not visited. if returned from visitElement,
   * only postVisit will be invoked and not its children. If returned from
   * postVisit, no-op behaviour, same as CONTINUE
   */
  SKIP_SUBTREE,

  /**
   * Continue without visiting the unvisited siblings of this element.
   * If returned from the preVisitElement
   * method then the entries in the Element are also
   * skipped but the postVisitElement method is invoked.
   * If returned from the postVisitDirectory method, no further siblings
   * are visited
   */
  SKIP_SIBLINGS;
}
