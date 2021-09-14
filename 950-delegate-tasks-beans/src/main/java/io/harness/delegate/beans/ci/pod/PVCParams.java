/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.beans.ci.pod;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

@Data
@Builder
@AllArgsConstructor
public class PVCParams {
  @NonNull private String volumeName;
  @NonNull private String claimName;
  private boolean isPresent; // Whether PVC is already present on the PVC. If false, PVC will be created on the cluster.
  private String storageClass; // Storage class of the PVC
  private Integer sizeMib; // Size of PVC in Mebibytes
}
