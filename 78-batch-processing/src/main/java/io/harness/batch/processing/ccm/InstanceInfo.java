package io.harness.batch.processing.ccm;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class InstanceInfo {
  String accountId;
  String cloudProviderId;
  String instanceId;
  InstanceType instanceType;
  Resource resource;
  List<Container> containerList;
  Map<String, String> labels;
}
