package io.harness.walktree.visitor.validation;

import io.harness.beans.ParameterField;
import io.harness.walktree.beans.LevelNode;
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
  @Required(groups = PreInputSet.class) ParameterField<String> parameterField;

  VisitorTestChild visitorTestChild;

  String metaData;

  @Override
  public VisitableChildren getChildrenToWalk() {
    VisitableChildren visitableChildren = VisitableChildren.builder().build();
    visitableChildren.add("visitorTestChild", visitorTestChild);
    return visitableChildren;
  }

  @Override
  public LevelNode getLevelNode() {
    return LevelNode.builder().qualifierName("dummyTestPOJO").build();
  }
}
