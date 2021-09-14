/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.graphql.schema.type.aggregation;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import lombok.Builder;
import lombok.Value;

/**
 * This class is an intermediate class to build the stack data point data structure.
 */
@Value
@Builder
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLIntermediateStackDataPoint {
  String groupBy1;
  QLReference key;
  Number value;

  public QLDataPoint getDataPoint() {
    return QLDataPoint.builder().key(key).value(value).build();
  }
}
