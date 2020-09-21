package io.harness.cdng.variables;

import io.harness.cdng.visitor.LevelNodeQualifierName;
import io.harness.cdng.visitor.helpers.deploymentstage.StageVariablesVisitorHelper;
import io.harness.walktree.beans.LevelNode;
import io.harness.walktree.beans.VisitableChildren;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;
import io.harness.yaml.core.PreviousStageAware;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.util.List;

@Data
@Builder
@SimpleVisitorHelper(helperClass = StageVariablesVisitorHelper.class)
public class StageVariables implements PreviousStageAware, Visitable {
  @Singular private List<Variable> variables;
  private String previousStageIdentifier;
  @Singular private List<Variable> overrides;

  @Override
  public VisitableChildren getChildrenToWalk() {
    VisitableChildren visitableChildren = VisitableChildren.builder().build();
    // this will change once variables are properly implemented
    variables.forEach(variable -> visitableChildren.add("variables", variable));
    return visitableChildren;
  }

  @Override
  public LevelNode getLevelNode() {
    return LevelNode.builder().qualifierName(LevelNodeQualifierName.STAGE_VARIABLES).build();
  }
}
