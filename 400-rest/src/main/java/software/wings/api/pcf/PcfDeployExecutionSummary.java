/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.api.pcf;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.pcf.CfServiceData;

import software.wings.api.DeploymentInfo;
import software.wings.api.PcfDeploymentInfo;
import software.wings.service.impl.instance.DeploymentInfoExtractor;
import software.wings.sm.StepExecutionSummary;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@Slf4j
@OwnedBy(CDP)
public class PcfDeployExecutionSummary extends StepExecutionSummary implements DeploymentInfoExtractor {
  private String releaseName;
  private List<CfServiceData> instaceData;

  @Override
  public Optional<List<DeploymentInfo>> extractDeploymentInfo() {
    if (isEmpty(instaceData)) {
      log.warn(
          "Both old and new app resize details are empty. Cannot proceed for phase step for state execution instance");
      return Optional.empty();
    }

    List<DeploymentInfo> pcfDeploymentInfo = new ArrayList<>();
    instaceData.forEach(cfServiceData
        -> pcfDeploymentInfo.add(PcfDeploymentInfo.builder()
                                     .applicationName(cfServiceData.getName())
                                     .applicationGuild(cfServiceData.getId())
                                     .build()));
    return Optional.of(pcfDeploymentInfo);
  }
}
