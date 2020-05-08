package io.harness.beans.stages;

import graph.Graph;
import io.harness.beans.steps.CIStep;
import lombok.Builder;
import lombok.Data;
import lombok.Value;

/**
 *  This Stage stores steps required for running CI job
 */

@Data
@Value
@Builder
public class JobStage implements StageInfo {
  Graph<CIStep> stepInfos;
}
