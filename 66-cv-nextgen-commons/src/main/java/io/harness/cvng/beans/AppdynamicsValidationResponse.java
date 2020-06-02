package io.harness.cvng.beans;

import io.harness.cvng.models.ThirdPartyApiResponseStatus;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Singular;

import java.util.List;

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
