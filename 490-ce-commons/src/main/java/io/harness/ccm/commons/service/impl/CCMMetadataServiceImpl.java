package io.harness.ccm.commons.service.impl;

import io.harness.ccm.commons.entities.ClusterRecord;
import io.harness.ccm.commons.service.intf.CCMMetadataService;

public class CCMMetadataServiceImpl implements CCMMetadataService {
  @Override
  public boolean isCloudCostEnabled(ClusterRecord clusterRecord) {
    return true;
  }
}
