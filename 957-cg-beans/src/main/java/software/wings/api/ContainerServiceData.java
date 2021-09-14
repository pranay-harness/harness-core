/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.api;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Data;

/**
 * Created by bzane on 9/11/17.
 */
@Data
@Builder
@OwnedBy(CDP)
public class ContainerServiceData {
  private String name;
  private String image;
  // Use this if name can not be unique, like in case of ECS daemonSet
  private String uniqueIdentifier;
  private int previousCount;
  private int desiredCount;
  private int previousTraffic;
  private int desiredTraffic;
}
