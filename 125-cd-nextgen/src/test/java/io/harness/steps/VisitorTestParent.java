/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.steps;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.walktree.beans.VisitableChildren;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;

import lombok.Builder;
import lombok.Data;

@OwnedBy(PIPELINE)
@Data
@Builder
@SimpleVisitorHelper(helperClass = VisitorTestParentVisitorHelper.class)
public class VisitorTestParent implements Visitable {
  String name;
  VisitorTestChild visitorTestChild;
  ConnectorRefChild connectorRefChild;

  @Override
  public VisitableChildren getChildrenToWalk() {
    VisitableChildren visitableChildren = VisitableChildren.builder().build();
    visitableChildren.add("visitorTestChild", visitorTestChild);
    visitableChildren.add("connectorRefChild", connectorRefChild);
    return visitableChildren;
  }
}
