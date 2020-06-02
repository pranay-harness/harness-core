package io.harness.cdng.pipeline.beans;

import io.harness.state.io.StepParameters;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class StagesStepParameters implements StepParameters {
  private List<String> stageNodeIds;
}
