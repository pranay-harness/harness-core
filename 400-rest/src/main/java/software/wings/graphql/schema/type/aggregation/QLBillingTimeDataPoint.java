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

@Value
@Builder
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLBillingTimeDataPoint {
  QLReference key;
  Number value;
  long time;

  public QLBillingDataPoint getQLBillingDataPoint() {
    return QLBillingDataPoint.builder().value(value).key(key).build();
  }
}
