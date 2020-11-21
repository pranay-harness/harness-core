package io.harness.cvng.analysis.entities;

import static io.harness.cvng.analysis.entities.LearningEngineTask.LearningEngineTaskType.LOG_CLUSTER;

import io.harness.cvng.analysis.beans.LogClusterLevel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class LogClusterLearningEngineTask extends LearningEngineTask {
  private String testDataUrl;
  private String host;
  private LogClusterLevel clusterLevel;
  @Override
  public LearningEngineTaskType getType() {
    return LOG_CLUSTER;
  }
}
