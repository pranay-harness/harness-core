package io.harness.ng.core.dto;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import java.util.Set;
import lombok.Builder;
import lombok.Value;

@OwnedBy(PL)
@Value
@Builder
public class AggregateACLRequest {
  String searchTerm;
  Set<String> resourceGroupFilter;
  Set<String> roleFilter;
}
