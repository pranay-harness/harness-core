package io.harness.pms.sdk.core;

import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FilterCreatorMergeServiceResponse {
  Map<String, String> filters;
  int stageCount;
}
