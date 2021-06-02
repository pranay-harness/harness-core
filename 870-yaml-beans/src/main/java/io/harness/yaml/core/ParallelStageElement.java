package io.harness.yaml.core;

import io.harness.visitor.helpers.stage.ParallelStageElementVisitorHelper;
import io.harness.walktree.beans.VisitableChildren;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;
import io.harness.yaml.core.auxiliary.intfc.StageElementWrapper;
import io.harness.yaml.core.serializer.ParallelStageElementSerializer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonTypeName("parallel")
@JsonSerialize(using = ParallelStageElementSerializer.class)
@SimpleVisitorHelper(helperClass = ParallelStageElementVisitorHelper.class)
public class ParallelStageElement implements StageElementWrapper, Visitable {
  @NotNull List<StageElementWrapper> sections;

  @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
  public ParallelStageElement(List<StageElementWrapper> sections) {
    this.sections = sections;
  }

  @Override
  public VisitableChildren getChildrenToWalk() {
    VisitableChildren visitableChildren = VisitableChildren.builder().build();
    sections.forEach(section -> visitableChildren.add("sections", section));
    return visitableChildren;
  }
}
