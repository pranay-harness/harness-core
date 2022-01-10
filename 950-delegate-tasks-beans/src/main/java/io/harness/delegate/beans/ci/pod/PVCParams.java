/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
