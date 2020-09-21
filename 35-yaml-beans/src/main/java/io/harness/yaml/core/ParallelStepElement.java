package io.harness.yaml.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.harness.visitor.helpers.executionelement.ParallelStepElementVisitorHelper;
import io.harness.walktree.beans.LevelNode;
import io.harness.walktree.beans.VisitableChildren;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;
import io.harness.yaml.core.auxiliary.intfc.ExecutionWrapper;
import io.harness.yaml.core.serializer.ParallelStepElementSerializer;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import javax.validation.constraints.NotNull;

/**
 * Parallel structure is special list of steps that can be executed in parallel.
 */
@Data
@Builder
@NoArgsConstructor
@JsonTypeName("parallel")
@JsonSerialize(using = ParallelStepElementSerializer.class)
@SimpleVisitorHelper(helperClass = ParallelStepElementVisitorHelper.class)
public class ParallelStepElement implements ExecutionWrapper, Visitable {
  @NotNull List<ExecutionWrapper> sections;

  @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
  public ParallelStepElement(List<ExecutionWrapper> sections) {
    this.sections = sections;
  }

  @Override
  public VisitableChildren getChildrenToWalk() {
    VisitableChildren visitableChildren = VisitableChildren.builder().build();
    for (ExecutionWrapper section : sections) {
      visitableChildren.add("sections", section);
    }
    return visitableChildren;
  }

  @Override
  public LevelNode getLevelNode() {
    return LevelNode.builder().qualifierName(LevelNodeQualifierName.PARALLEL_STEP_ELEMENT).build();
  }
}
