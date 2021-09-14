/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.analysis.beans;

import io.harness.cvng.beans.job.VerificationJobType;

import com.google.common.base.Preconditions;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CanaryAdditionalInfo extends CanaryBlueGreenAdditionalInfo {
  @Override
  public VerificationJobType getType() {
    return VerificationJobType.CANARY;
  }

  public enum CanaryAnalysisType {
    CLASSIC("primary", "canary"),
    IMPROVISED("before", "after"),
    BLUE_GREEN("blue", "green");

    private final String primaryInstancesLabel;
    private final String canaryInstancesLabel;

    CanaryAnalysisType(String primaryInstancesLabel, String canaryInstancesLabel) {
      this.primaryInstancesLabel = primaryInstancesLabel;
      this.canaryInstancesLabel = canaryInstancesLabel;
    }

    public String getCanaryInstancesLabel() {
      return this.canaryInstancesLabel;
    }

    public String getPrimaryInstancesLabel() {
      return this.primaryInstancesLabel;
    }
  }

  public void setFieldNames() {
    Preconditions.checkNotNull(this.primary, "Populate control hosts before setting field names");
    Preconditions.checkNotNull(this.canary, "Populate test hosts before setting field names");

    Set<String> controlHosts = this.getPrimary().stream().map(HostSummaryInfo::getHostName).collect(Collectors.toSet());
    Set<String> testHosts = this.getCanary().stream().map(HostSummaryInfo::getHostName).collect(Collectors.toSet());
    testHosts.removeAll(controlHosts);

    CanaryAnalysisType canaryAnalysisType =
        testHosts.size() > 0 ? CanaryAnalysisType.CLASSIC : CanaryAnalysisType.IMPROVISED;
    this.setPrimaryInstancesLabel(canaryAnalysisType.getPrimaryInstancesLabel());
    this.setCanaryInstancesLabel(canaryAnalysisType.getCanaryInstancesLabel());
  }
}
