package io.harness.pms.filter.creation;

import io.harness.pms.contracts.plan.FilterCreationBlobResponse;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FilterCreationResponseWrapper {
  String serviceName;
  FilterCreationBlobResponse response;
}
