/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
