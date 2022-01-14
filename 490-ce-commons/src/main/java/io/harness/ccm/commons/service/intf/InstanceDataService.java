package io.harness.ccm.commons.service.intf;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.commons.entities.batch.InstanceData;

import java.util.List;

@OwnedBy(CE)
public interface InstanceDataService {
  InstanceData get(String instanceId);
  List<InstanceData> fetchInstanceDataForGivenInstances(List<String> instanceIds);
  List<InstanceData> fetchInstanceDataForGivenInstances(String accountId, String clusterId, List<String> instanceIds);
}
