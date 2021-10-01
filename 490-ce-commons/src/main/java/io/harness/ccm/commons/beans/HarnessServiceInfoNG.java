package io.harness.ccm.commons.beans;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@AllArgsConstructor
public class HarnessServiceInfoNG {
  String serviceId;
  String orgIdentifier;
  String projectIdentifier;
  String envId;
  String infraMappingId;
}
