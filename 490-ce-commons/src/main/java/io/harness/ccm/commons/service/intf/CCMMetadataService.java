package io.harness.ccm.commons.service.intf;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.commons.entities.ClusterRecord;

@OwnedBy(CE)
public interface CCMMetadataService {
  boolean isCloudCostEnabled(ClusterRecord clusterRecord);
}
