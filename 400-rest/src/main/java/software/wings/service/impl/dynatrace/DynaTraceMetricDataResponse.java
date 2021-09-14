/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl.dynatrace;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;
import lombok.Data;

/**
 * Created by rsingh on 2/6/18.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DynaTraceMetricDataResponse {
  private DynaTraceMetricDataResult result;

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class DynaTraceMetricDataResult {
    private Map<String, List<List<Double>>> dataPoints;
    private String timeseriesId;
    private Map<String, String> entities;
    private String host;
  }
}
