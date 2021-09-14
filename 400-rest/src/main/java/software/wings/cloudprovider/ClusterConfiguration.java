/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.cloudprovider;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by anubhaw on 12/29/16.
 */
@OwnedBy(CDP)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClusterConfiguration {
  private Integer size;
  private String name;
}
