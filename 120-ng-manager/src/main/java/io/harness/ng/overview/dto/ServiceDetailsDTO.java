package io.harness.ng.overview.dto;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.models.dashboard.InstanceCountDetailsByEnvTypeBase;
import java.util.Map;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@OwnedBy(HarnessTeam.DX)
@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ServiceDetailsDTO {
  String serviceName;
  String serviceIdentifier;
  String description;
  Map<String, String> tags;
  Set<String> deploymentTypeList;
  long totalDeployments;
  double totalDeploymentChangeRate;
  double successRate;
  double successRateChangeRate;
  double failureRate;
  double failureRateChangeRate;
  double frequency;
  double frequencyChangeRate;
  InstanceCountDetailsByEnvTypeBase instanceCountDetails;
  ServicePipelineInfo lastPipelineExecuted;
}
