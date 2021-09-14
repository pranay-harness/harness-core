/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl.newrelic;

import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * Created by rsingh on 9/5/17.
 */
@Data
@Builder
public class NewRelicMetricResponse {
  private List<NewRelicMetric> metrics;
}
