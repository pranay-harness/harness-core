/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing.billing.timeseries.data;

import java.util.Set;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NodePodId {
  private String nodeId;
  private String clusterId;
  private Set<String> podId;
}
