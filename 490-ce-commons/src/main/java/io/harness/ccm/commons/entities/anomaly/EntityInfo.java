/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ccm.commons.entities.anomaly;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EntityInfo {
  String clusterName;
  String clusterId;
  String namespace;
  String workloadName;
  String workloadType;
  String gcpProject;
  String gcpProduct;
  String gcpSKUId;
  String gcpSKUDescription;
  String awsAccount;
  String awsService;
}
