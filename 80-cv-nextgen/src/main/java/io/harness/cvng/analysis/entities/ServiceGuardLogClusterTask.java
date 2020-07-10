package io.harness.cvng.analysis.entities;

import static io.harness.cvng.analysis.entities.LearningEngineTask.LearningEngineTaskType.SERVICE_GUARD_LOG_CLUSTER;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceGuardLogClusterTask extends LearningEngineTask {
  private String testDataUrl;
  private String host;
  @Override
  public LearningEngineTaskType getType() {
    return SERVICE_GUARD_LOG_CLUSTER;
  }
}
