/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.core.entities;

import java.util.Objects;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class AnalysisInfo {
  private String identifier;
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
    // TODO: Make it healthSource type specific
    String serviceInstanceMetricPath;
  }

  @Data
  @Builder
  public static class SLI {
    boolean enabled;
  }
}
