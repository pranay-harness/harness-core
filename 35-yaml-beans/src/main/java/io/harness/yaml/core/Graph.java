package io.harness.yaml.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.harness.visitor.helpers.executionelement.GraphVisitorHelper;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;
import io.harness.yaml.core.auxiliary.intfc.ExecutionWrapper;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;

/**
 * Graph structure is special list of steps that can be represented in form of dependencies.
 * Each step will have a list of other step identifiers that it depends on.
 */
@Value
@Builder
@SimpleVisitorHelper(helperClass = GraphVisitorHelper.class)
public class Graph implements ExecutionWrapper, Visitable {
  @NotNull List<StepElement> sections;

  @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
  public Graph(List<StepElement> graph) {
    this.sections = graph;
  }

  @Override
  public List<Object> getChildrenToWalk() {
    return sections.stream().map(step -> (Object) step).collect(Collectors.toList());
  }
}
