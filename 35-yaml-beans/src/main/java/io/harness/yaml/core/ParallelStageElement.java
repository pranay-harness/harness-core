package io.harness.yaml.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.harness.yaml.core.auxiliary.intfc.StageElementWrapper;
import io.harness.yaml.core.serializer.ParallelStageElementSerializer;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import javax.validation.constraints.NotNull;

@Value
@Builder
@JsonTypeName("parallel")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSerialize(using = ParallelStageElementSerializer.class)
public class ParallelStageElement implements StageElementWrapper {
  @NotNull List<StageElementWrapper> sections;

  @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
  public ParallelStageElement(List<StageElementWrapper> sections) {
    this.sections = sections;
  }
}
