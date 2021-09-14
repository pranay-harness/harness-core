/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.beans;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Singular;

@Data
@Builder
@EqualsAndHashCode(of = {"metricPackName"})
public class AppdynamicsValidationResponse {
  private String metricPackName;
  private ThirdPartyApiResponseStatus overallStatus;
  @Singular("addValidationResponse") private List<AppdynamicsMetricValueValidationResponse> values;

  @Data
  @Builder
  @EqualsAndHashCode(of = {"metricName"})
  public static class AppdynamicsMetricValueValidationResponse {
    private String metricName;
    private ThirdPartyApiResponseStatus apiResponseStatus;
    private double value;
    private String errorMessage;
  }
}
