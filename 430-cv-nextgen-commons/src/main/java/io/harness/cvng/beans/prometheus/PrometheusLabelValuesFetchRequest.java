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
import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@JsonTypeName("PROMETHEUS_LABEL_VALUES_GET")
@Data
@SuperBuilder
@NoArgsConstructor
@OwnedBy(CV)
public class PrometheusLabelValuesFetchRequest extends PrometheusRequest {
  public static final String DSL = PrometheusLabelValuesFetchRequest.readDSL(
      "prometheus-label-values.datacollection", PrometheusMetricListFetchRequest.class);

  private String labelName;

  @Override
  public String getDSL() {
    return DSL;
  }

  @Override
  public Map<String, Object> fetchDslEnvVariables() {
    Map<String, Object> collectionEnvs = new HashMap<>();
    collectionEnvs.put("labelName", labelName);
    return collectionEnvs;
  }
}
