/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing.config.k8s.resource.change;

import lombok.Value;

@Value(staticConstructor = "of")
public class WorkloadId {
  String clusterId;
  String uid;
}
