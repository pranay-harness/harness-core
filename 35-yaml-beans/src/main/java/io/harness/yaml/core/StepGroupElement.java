package io.harness.yaml.core;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.visitor.helpers.executionelement.StepGroupElementVisitorHelper;
import io.harness.walktree.beans.VisitableChildren;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;
import io.harness.yaml.core.auxiliary.intfc.ExecutionWrapper;
import io.harness.yaml.core.intfc.WithIdentifier;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@JsonTypeName("stepGroup")
@SimpleVisitorHelper(helperClass = StepGroupElementVisitorHelper.class)
public class StepGroupElement implements ExecutionWrapper, WithIdentifier, Visitable {
  String identifier;
  String name;
  @NotNull List<ExecutionWrapper> steps;
  List<ExecutionWrapper> rollbackSteps;

  @Override
  public VisitableChildren getChildrenToWalk() {
    VisitableChildren children = VisitableChildren.builder().build();
    steps.forEach(step -> children.add("steps", step));
    rollbackSteps.forEach(rollbackStep -> children.add("rollbackSteps", rollbackStep));
    return children;
  }
}
