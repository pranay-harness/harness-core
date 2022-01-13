/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
