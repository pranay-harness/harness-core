package io.harness.yaml.core;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.data.validator.EntityIdentifier;
import io.harness.data.validator.EntityName;
import io.harness.visitor.helpers.executionelement.StepGroupElementVisitorHelper;
import io.harness.walktree.beans.LevelNode;
import io.harness.walktree.beans.VisitableChildren;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;
import io.harness.yaml.core.auxiliary.intfc.ExecutionWrapper;
import io.harness.yaml.core.intfc.WithIdentifier;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import javax.validation.constraints.NotNull;

@Data
@Builder
@AllArgsConstructor
@JsonTypeName("stepGroup")
@SimpleVisitorHelper(helperClass = StepGroupElementVisitorHelper.class)
public class StepGroupElement implements ExecutionWrapper, WithIdentifier, Visitable {
  @EntityIdentifier String identifier;
  @EntityName String name;
  @NotNull List<ExecutionWrapper> steps;
  List<ExecutionWrapper> rollbackSteps;

  // For Visitor Framework Impl
  String metadata;

  @Override
  public VisitableChildren getChildrenToWalk() {
    VisitableChildren children = VisitableChildren.builder().build();
    steps.forEach(step -> children.add("steps", step));
    rollbackSteps.forEach(rollbackStep -> children.add("rollbackSteps", rollbackStep));
    return children;
  }

  @Override
  public LevelNode getLevelNode() {
    return LevelNode.builder().qualifierName(LevelNodeQualifierName.STEP_GROUP).build();
  }
}
