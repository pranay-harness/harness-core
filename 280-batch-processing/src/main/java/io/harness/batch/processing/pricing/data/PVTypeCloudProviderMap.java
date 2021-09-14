/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing.pricing.data;

import io.harness.ccm.commons.constants.CloudProvider;
import io.harness.perpetualtask.k8s.watch.PVInfo.PVType;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * One cloud provider may have multiple Persistent Volume Types.
 */
@Slf4j
public enum PVTypeCloudProviderMap {
  GCE_PD(PVType.PV_TYPE_GCE_PERSISTENT_DISK, io.harness.ccm.commons.constants.CloudProvider.GCP),
  AWS_EBS(PVType.PV_TYPE_AWS_EBS, io.harness.ccm.commons.constants.CloudProvider.AWS),
  AZURE_DISK(PVType.PV_TYPE_AZURE_DISK, io.harness.ccm.commons.constants.CloudProvider.AZURE),
  NFS(PVType.PV_TYPE_NFS, io.harness.ccm.commons.constants.CloudProvider.ON_PREM),
  UNKNOWN(PVType.PV_TYPE_UNSPECIFIED, io.harness.ccm.commons.constants.CloudProvider.UNKNOWN);

  @Getter private final PVType pvType;
  @Getter private final io.harness.ccm.commons.constants.CloudProvider cloudProvider;
  PVTypeCloudProviderMap(PVType pvType, io.harness.ccm.commons.constants.CloudProvider cloudProvider) {
    this.pvType = pvType;
    this.cloudProvider = cloudProvider;
  }

  public static io.harness.ccm.commons.constants.CloudProvider get(PVType pvType) {
    for (PVTypeCloudProviderMap val : PVTypeCloudProviderMap.values()) {
      if (val.getPvType() == pvType) {
        return val.getCloudProvider();
      }
    }
    return UNKNOWN.getCloudProvider();
  }
  public static CloudProvider get(String pvTypeString) {
    try {
      return get(PVType.valueOf(pvTypeString));
    } catch (Exception ex) {
      log.error("Failed to parse PVType, unrecognized {}", pvTypeString);
      return get(PVType.PV_TYPE_UNSPECIFIED);
    }
  }
}
