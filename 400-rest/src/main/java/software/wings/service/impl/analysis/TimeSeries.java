/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl.analysis;

import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by rsingh on 3/14/18.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeSeries {
  @NotNull private String txnName;
  @NotNull private String url;
  @NotNull private String metricName;
  @NotNull private String metricType;
}
