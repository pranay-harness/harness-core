package io.harness.yaml.core;

import io.harness.visitor.helpers.executionelement.ExecutionElementVisitorHelper;
import io.harness.walktree.beans.VisitableChildren;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;
import io.harness.yaml.core.auxiliary.intfc.ExecutionWrapper;
import lombok.Builder;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;

import java.beans.ConstructorProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Execution represents list of steps that can be used within stage
 * Steps can be also represented in special list of steps called parallel or graph
 * {@link ExecutionElement} is used to represent this dynamic property where object in this list
 * can be another list of steps or single step
 */
@Value
@Builder
@SimpleVisitorHelper(helperClass = ExecutionElementVisitorHelper.class)
public class ExecutionElement implements Visitable {
  @NotEmpty List<ExecutionWrapper> steps;
  List<ExecutionWrapper> rollbackSteps;

  @ConstructorProperties({"steps", "rollbackSteps"})
  public ExecutionElement(List<ExecutionWrapper> steps, List<ExecutionWrapper> rollbackSteps) {
    this.steps = Optional.ofNullable(steps).orElse(new ArrayList<>());
    this.rollbackSteps = Optional.ofNullable(rollbackSteps).orElse(new ArrayList<>());
  }

  @Override
  public VisitableChildren getChildrenToWalk() {
    VisitableChildren children = VisitableChildren.builder().build();
    steps.forEach(step -> children.add("steps", step));
    rollbackSteps.forEach(step -> children.add("rollbackSteps", step));
    return children;
  }
}
