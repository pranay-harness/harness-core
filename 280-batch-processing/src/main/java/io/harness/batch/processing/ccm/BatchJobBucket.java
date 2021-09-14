/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing.ccm;

public enum BatchJobBucket {
  OUT_OF_CLUSTER,
  IN_CLUSTER,
  IN_CLUSTER_BILLING,
  IN_CLUSTER_RECOMMENDATION,
  IN_CLUSTER_NODE_RECOMMENDATION,
  OTHERS
}
