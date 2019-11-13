package io.harness.batch.processing.ccm;

import lombok.Builder;
import lombok.Data;
import software.wings.beans.instance.HarnessServiceInfo;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class InstanceInfo {
  String accountId;
  String cloudProviderId;
  String instanceId;
  String instanceName;
  InstanceType instanceType;
  Resource resource;
  List<Container> containerList;
  Map<String, String> labels;
  Map<String, String> metaData;
  HarnessServiceInfo harnessServiceInfo;
}
