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
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@JsonTypeName("PROMETHEUS_SAMPLE_DATA")
@Data
@SuperBuilder
@NoArgsConstructor
@OwnedBy(CV)
public class PrometheusFetchSampleDataRequest extends PrometheusRequest {
  public static final String DSL = PrometheusFetchSampleDataRequest.readDSL(
      "prometheus-sample-data.datacollection", PrometheusMetricListFetchRequest.class);

  private String query;
  private Instant startTime;
  private Instant endTime;

  @Override
  public String getDSL() {
    return DSL;
  }

  @Override
  public Map<String, Object> fetchDslEnvVariables() {
    Map<String, Object> collectionEnvs = new HashMap<>();
    collectionEnvs.put("query", query);
    collectionEnvs.put("startTime", startTime.getEpochSecond());
    collectionEnvs.put("endTime", endTime.getEpochSecond());
    return collectionEnvs;
  }
}
