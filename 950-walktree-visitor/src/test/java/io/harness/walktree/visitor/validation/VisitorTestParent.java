/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.walktree.visitor.validation;

import io.harness.walktree.beans.DummyVisitorField;
import io.harness.walktree.beans.VisitableChildren;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;
import io.harness.walktree.visitor.validation.annotations.Required;
import io.harness.walktree.visitor.validation.modes.PreInputSet;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@SimpleVisitorHelper(helperClass = VisitorTestParentVisitorHelper.class)
public class VisitorTestParent implements Visitable {
  // used to check normal string case handling
  @Required(groups = PreInputSet.class) String name;

  // used to check for parameterField
  @Required(groups = PreInputSet.class) DummyVisitorField visitorField;

  VisitorTestChild visitorTestChild;

  String metaData;

  @Override
  public VisitableChildren getChildrenToWalk() {
    VisitableChildren visitableChildren = VisitableChildren.builder().build();
    visitableChildren.add("visitorTestChild", visitorTestChild);
    return visitableChildren;
  }
}
