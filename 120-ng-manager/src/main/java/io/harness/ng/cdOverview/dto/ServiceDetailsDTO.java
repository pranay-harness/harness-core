package io.harness.ng.cdOverview.dto;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.List;
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
  List<String> deploymentTypeList;
  long totalDeployments;
  double totalDeploymentChangeRate;
  double successRate;
  double successRateChangeRate;
  double failureRate;
  double failureRateChangeRate;
  double frequency;
  double frequencyChangeRate;
  ServicePipelineInfo lastPipelineExecuted;
}
