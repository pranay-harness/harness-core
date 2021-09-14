/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.statemachine.entities;

import io.harness.cvng.statemachine.beans.AnalysisInput;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Data
@Builder
@NoArgsConstructor
@Slf4j
public class ServiceGuardTimeSeriesAnalysisState extends TimeSeriesAnalysisState {
  @Override
  protected List<String> scheduleAnalysis(AnalysisInput analysisInput) {
    return timeSeriesAnalysisService.scheduleServiceGuardAnalysis(analysisInput);
  }
}
