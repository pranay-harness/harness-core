/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.api.pcf;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.pcf.CfAppSetupTimeDetails;
import io.harness.delegate.beans.pcf.CfRouteUpdateRequestConfigData;

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

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@OwnedBy(CDP)
public class PcfRouteSwapExecutionSummary extends StepExecutionSummary implements DeploymentInfoExtractor {
  private String organization;
  private String space;
  private CfRouteUpdateRequestConfigData pcfRouteUpdateRequestConfigData;

  @Override
  public Optional<List<DeploymentInfo>> extractDeploymentInfo() {
    if (pcfRouteUpdateRequestConfigData.isDownsizeOldApplication()) {
      List<DeploymentInfo> pcfDeploymentInfo = new ArrayList<>();
      List<CfAppSetupTimeDetails> details = pcfRouteUpdateRequestConfigData.getExistingApplicationDetails();
      details.forEach(existingApp
          -> pcfDeploymentInfo.add(PcfDeploymentInfo.builder()
                                       .applicationName(existingApp.getApplicationName())
                                       .applicationGuild(existingApp.getApplicationGuid())
                                       .build()));
      return Optional.of(pcfDeploymentInfo);
    }
    return Optional.empty();
  }
}
