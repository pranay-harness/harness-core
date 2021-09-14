/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.beans.prometheus;

import static io.harness.annotations.dev.HarnessTeam.CV;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@JsonTypeName("PROMETHEUS_METRIC_LIST_GET")
@Data
@SuperBuilder
@NoArgsConstructor
@OwnedBy(CV)
public class PrometheusMetricListFetchRequest extends PrometheusRequest {
  public static final String DSL = PrometheusMetricListFetchRequest.readDSL(
      "prometheus-metric-list.datacollection", PrometheusMetricListFetchRequest.class);

  @Override
  public String getDSL() {
    return DSL;
  }
}
