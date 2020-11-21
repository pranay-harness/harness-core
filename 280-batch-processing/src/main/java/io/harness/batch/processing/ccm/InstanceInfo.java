package io.harness.batch.processing.ccm;

import io.harness.ccm.commons.beans.Container;
import io.harness.ccm.commons.beans.HarnessServiceInfo;
import io.harness.ccm.commons.beans.InstanceState;
import io.harness.ccm.commons.beans.InstanceType;
import io.harness.ccm.commons.beans.Resource;
import io.harness.ccm.commons.beans.StorageResource;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InstanceInfo {
  String accountId;
  String settingId;
  String instanceId;
  String clusterId;
  String clusterName;
  String instanceName;
  String cloudProviderInstanceId;
  InstanceType instanceType;
  InstanceState instanceState;
  Instant usageStartTime;
  Resource resource;
  Resource resourceLimit;
  Resource allocatableResource;
  StorageResource storageResource;
  List<Container> containerList;
  Map<String, String> labels;
  Map<String, String> namespaceLabels;
  Map<String, String> metaData;
  HarnessServiceInfo harnessServiceInfo;
}
