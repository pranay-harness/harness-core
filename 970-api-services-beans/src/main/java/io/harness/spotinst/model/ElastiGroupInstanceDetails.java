/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.spotinst.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ElastiGroupInstanceDetails {
  private String instanceId;
  private String instanceType;
  private String product;
  private String groupId;
  private String availabilityZone;
  private String privateIp;
  private String publicIp;
}
