package io.harness.yaml.core;

import io.harness.visitor.helpers.executionelement.GraphVisitorHelper;
import io.harness.walktree.beans.VisitableChildren;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;
import io.harness.yaml.core.auxiliary.intfc.ExecutionWrapper;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

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
  public VisitableChildren getChildrenToWalk() {
    VisitableChildren visitableChildren = VisitableChildren.builder().build();
    sections.forEach(step -> { visitableChildren.add("sections", step); });
    return visitableChildren;
  }
}
