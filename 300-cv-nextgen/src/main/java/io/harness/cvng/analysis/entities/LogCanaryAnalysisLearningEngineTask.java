package io.harness.cvng.analysis.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class LogCanaryAnalysisLearningEngineTask extends LearningEngineTask {
  private String controlDataUrl;
  private String testDataUrl;
  private Set<String> controlHosts;

  public Set<String> getControlHosts() {
    if (controlHosts == null) {
      return Collections.emptySet();
    }
    return controlHosts;
  }
  @Override
  public LearningEngineTaskType getType() {
    return LearningEngineTaskType.CANARY_LOG_ANALYSIS;
  }
}
