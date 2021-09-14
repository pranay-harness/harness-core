/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.core.beans.params.filterParams;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.cvng.analysis.entities.LogAnalysisResult.LogAnalysisTag;

import java.util.List;
import lombok.AccessLevel;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@FieldDefaults(level = AccessLevel.PRIVATE)
@SuperBuilder
@Value
public class LiveMonitoringLogAnalysisFilter extends LogAnalysisFilter {
  List<LogAnalysisTag> clusterTypes;

  public boolean filterByClusterTypes() {
    return isNotEmpty(clusterTypes);
  }
}
