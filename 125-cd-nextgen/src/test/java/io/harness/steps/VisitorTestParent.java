/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
