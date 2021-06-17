package io.harness.ng.serviceaccounts.dto;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
@OwnedBy(PL)
public class ServiceAccountRequestDTO {
  String identifier;
  String name;
  String description;
}
