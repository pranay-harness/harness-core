/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.core.beans;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.Value;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class PrometheusSampleData {
  Map<String, String> metricDetails;
  List<List<Double>> data;

  public List<DataPoint> getData() {
    List<DataPoint> dataPoints = new ArrayList<>();
    if (isNotEmpty(data)) {
      data.forEach(listItem -> {
        Preconditions.checkState(listItem.size() == 2);
        dataPoints.add(DataPoint.builder().timestamp(listItem.get(0).longValue()).value(listItem.get(1)).build());
      });
      return dataPoints;
    }
    return null;
  }

  @Data
  @Builder
  public static class DataPoint {
    long timestamp;
    double value;

    public void setValue(String val) {
      this.value = Double.valueOf(val);
    }
  }
}
