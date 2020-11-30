package io.harness.pms.sdk.core;

import io.harness.pms.plan.FilterCreationBlobResponse;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FilterCreationResponseWrapper {
  String serviceName;
  FilterCreationBlobResponse response;
}
