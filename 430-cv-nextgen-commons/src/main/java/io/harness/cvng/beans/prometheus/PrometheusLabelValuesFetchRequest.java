package io.harness.cvng.beans.prometheus;

import static io.harness.annotations.dev.HarnessTeam.CV;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.beans.stackdriver.StackdriverDashboardRequest;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
@JsonTypeName("PROMETHEUS_LABEL_VALUES_GET")
@Data
@SuperBuilder
@NoArgsConstructor
@OwnedBy(CV)
public class PrometheusLabelValuesFetchRequest extends PrometheusRequest {
  public static final String DSL = StackdriverDashboardRequest.readDSL(
      "prometheus-metric-list.datacollection", PrometheusMetricListFetchRequest.class);

  @Override
  public String getDSL() {
    return null;
  }
}
