package io.harness.yaml.core;

import io.harness.visitor.helpers.executionelement.ExecutionElementVisitorHelper;
import io.harness.walktree.beans.VisitableChildren;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;
import io.harness.yaml.core.auxiliary.intfc.ExecutionWrapper;

import java.beans.ConstructorProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.Builder;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.annotation.TypeAlias;

/**
 * Execution represents list of steps that can be used within stage
 * Steps can be also represented in special list of steps called parallel or graph
 * {@link ExecutionElement} is used to represent this dynamic property where object in this list
 * can be another list of steps or single step
 */
@Data
@Builder
@SimpleVisitorHelper(helperClass = ExecutionElementVisitorHelper.class)
@TypeAlias("io.harness.yaml.core.executionElement")
public class ExecutionElement implements Visitable {
  @NotEmpty List<ExecutionWrapper> steps;
  List<ExecutionWrapper> rollbackSteps;

  // For Visitor Framework Impl
  String metadata;

  @ConstructorProperties({"steps", "rollbackSteps", "metadata"})
  public ExecutionElement(List<ExecutionWrapper> steps, List<ExecutionWrapper> rollbackSteps, String metadata) {
    this.steps = Optional.ofNullable(steps).orElse(new ArrayList<>());
    this.rollbackSteps = Optional.ofNullable(rollbackSteps).orElse(new ArrayList<>());
    this.metadata = metadata;
  }

  @Override
  public VisitableChildren getChildrenToWalk() {
    VisitableChildren children = VisitableChildren.builder().build();
    steps.forEach(step -> children.add("steps", step));
    rollbackSteps.forEach(step -> children.add("rollbackSteps", step));
    return children;
  }
}
