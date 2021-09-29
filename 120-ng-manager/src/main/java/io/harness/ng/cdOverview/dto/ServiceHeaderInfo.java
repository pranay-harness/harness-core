package io.harness.ng.cdOverview.dto;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.Set;
import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.DX)
@Value
@Builder
public class ServiceHeaderInfo {
  String name;
  String identifier;
  String description;
  Set<String> deploymentTypes;
  Long createdAt;
  Long lastModifiedAt;
}
