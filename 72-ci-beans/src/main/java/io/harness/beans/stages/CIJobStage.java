package io.harness.beans.stages;

import graph.StepsGraph;
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
public class CIJobStage implements CIStageInfo {
  StepsGraph<CIStep> stepInfos;
}
