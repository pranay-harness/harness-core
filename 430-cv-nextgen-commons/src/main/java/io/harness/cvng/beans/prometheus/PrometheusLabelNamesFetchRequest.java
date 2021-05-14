package io.harness.cvng.beans.prometheus;

import static io.harness.annotations.dev.HarnessTeam.CV;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@JsonTypeName("PROMETHEUS_LABEL_NAMES_GET")
@Data
@SuperBuilder
@NoArgsConstructor
@OwnedBy(CV)
public class PrometheusLabelNamesFetchRequest extends PrometheusRequest {
  public static final String DSL = PrometheusLabelNamesFetchRequest.readDSL(
      "prometheus-label-names.datacollection", PrometheusLabelNamesFetchRequest.class);

  @Override
  public String getDSL() {
    return DSL;
  }
}
