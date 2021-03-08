package io.harness.batch.processing.pricing.data;

import io.harness.perpetualtask.k8s.watch.PVInfo.PVType;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * One cloud provider may have multiple Persistent Volume Types.
 */
@Slf4j
public enum PVTypeCloudProviderMap {
  GCE_PD(PVType.PV_TYPE_GCE_PERSISTENT_DISK, CloudProvider.GCP),
  AWS_EBS(PVType.PV_TYPE_AWS_EBS, CloudProvider.AWS),
  AZURE_DISK(PVType.PV_TYPE_AZURE_DISK, CloudProvider.AZURE),
  NFS(PVType.PV_TYPE_NFS, CloudProvider.ON_PREM),
  UNKNOWN(PVType.PV_TYPE_UNSPECIFIED, CloudProvider.UNKNOWN);

  @Getter private final PVType pvType;
  @Getter private final CloudProvider cloudProvider;
  PVTypeCloudProviderMap(PVType pvType, CloudProvider cloudProvider) {
    this.pvType = pvType;
    this.cloudProvider = cloudProvider;
  }

  public static CloudProvider get(PVType pvType) {
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