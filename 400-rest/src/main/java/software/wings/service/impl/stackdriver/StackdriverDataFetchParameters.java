/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl.stackdriver;

import software.wings.service.impl.ThirdPartyApiCallLog;

import com.google.api.services.monitoring.v3.Monitoring;
import java.util.List;
import java.util.Optional;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StackdriverDataFetchParameters {
  private String nameSpace;
  private String metric;
  private String dimensionValue;
  private String groupName;
  private String filter;
  private String projectId;
  private Monitoring monitoring;
  private Optional<List<String>> groupByFields;
  private Optional<String> perSeriesAligner;
  private Optional<String> crossSeriesReducer;
  private long startTime;
  private long endTime;
  private int dataCollectionMinute;
  private ThirdPartyApiCallLog apiCallLog;
  private boolean is247Task;
}
