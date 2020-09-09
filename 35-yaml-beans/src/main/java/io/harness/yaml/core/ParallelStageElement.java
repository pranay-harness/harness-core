package io.harness.yaml.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.harness.visitor.helpers.stage.ParallelStageElementVisitorHelper;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;
import io.harness.yaml.core.auxiliary.intfc.StageElementWrapper;
import io.harness.yaml.core.serializer.ParallelStageElementSerializer;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;

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
  public List<Object> getChildrenToWalk() {
    return sections.stream().map(stage -> (Object) stage).collect(Collectors.toList());
  }
}
