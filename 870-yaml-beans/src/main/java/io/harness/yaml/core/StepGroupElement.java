package io.harness.yaml.core;

import io.harness.beans.WithIdentifier;
import io.harness.data.structure.EmptyPredicate;
import io.harness.data.validator.EntityIdentifier;
import io.harness.data.validator.EntityName;
import io.harness.visitor.helpers.executionelement.StepGroupElementVisitorHelper;
import io.harness.walktree.beans.VisitableChildren;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;
import io.harness.yaml.core.auxiliary.intfc.ExecutionWrapper;
import io.harness.yaml.core.failurestrategy.FailureStrategyConfig;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@AllArgsConstructor
@JsonTypeName("stepGroup")
@SimpleVisitorHelper(helperClass = StepGroupElementVisitorHelper.class)
@TypeAlias("io.harness.yaml.core.stepGroupElement")
public class StepGroupElement implements ExecutionWrapper, WithIdentifier, Visitable {
  @EntityIdentifier String identifier;
  @EntityName String name;

  List<FailureStrategyConfig> failureStrategies;

  @NotNull List<ExecutionWrapper> steps;
  List<ExecutionWrapper> rollbackSteps;

  // For Visitor Framework Impl
  String metadata;

  @Override
  public VisitableChildren getChildrenToWalk() {
    VisitableChildren children = VisitableChildren.builder().build();
    if (EmptyPredicate.isNotEmpty(steps)) {
      steps.forEach(step -> children.add("steps", step));
    }
    if (EmptyPredicate.isNotEmpty(rollbackSteps)) {
      rollbackSteps.forEach(rollbackStep -> children.add("rollbackSteps", rollbackStep));
    }
    return children;
  }
}
