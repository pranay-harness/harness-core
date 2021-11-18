package io.harness.cvng.core.entities;

import java.util.Objects;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class AnalysisInfo {
  private LiveMonitoring liveMonitoring;
  private DeploymentVerification deploymentVerification;
  private SLI sli;

  public LiveMonitoring getLiveMonitoring() {
    if (Objects.isNull(liveMonitoring)) {
      return LiveMonitoring.builder().enabled(true).build();
    }
    return liveMonitoring;
  }

  public DeploymentVerification getDeploymentVerification() {
    if (Objects.isNull(deploymentVerification)) {
      return DeploymentVerification.builder().enabled(true).build();
    }
    return deploymentVerification;
  }

  public SLI getSli() {
    if (Objects.isNull(sli)) {
      return SLI.builder().enabled(true).build();
    }
    return sli;
  }

  @Data
  @Builder
  public static class LiveMonitoring {
    boolean enabled;
  }

  @Data
  @Builder
  public static class DeploymentVerification {
    boolean enabled;
  }

  @Data
  @Builder
  public static class SLI {
    boolean enabled;
  }
}
