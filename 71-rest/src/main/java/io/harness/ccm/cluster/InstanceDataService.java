package io.harness.ccm.cluster;

import io.harness.ccm.commons.entities.InstanceData;

import java.util.List;

public interface InstanceDataService {
  InstanceData get(String instanceId);
  List<InstanceData> fetchInstanceDataForGivenInstances(List<String> instanceIds);
  List<InstanceData> fetchInstanceDataForGivenInstances(String accountId, String clusterId, List<String> instanceIds);
}
