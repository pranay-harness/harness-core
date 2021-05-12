package io.harness.yaml.core;

import io.harness.visitor.helpers.executionelement.ParallelStepElementVisitorHelper;
import io.harness.walktree.beans.VisitableChildren;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;
import io.harness.yaml.core.auxiliary.intfc.ExecutionWrapper;
import io.harness.yaml.core.serializer.ParallelStepElementSerializer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

/**
 * Parallel structure is special list of steps that can be executed in parallel.
 */
@Data
@Builder
@NoArgsConstructor
@JsonTypeName("parallel")
@JsonSerialize(using = ParallelStepElementSerializer.class)
@SimpleVisitorHelper(helperClass = ParallelStepElementVisitorHelper.class)
@TypeAlias("io.harness.yaml.core.parallelStepElement")
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
}
