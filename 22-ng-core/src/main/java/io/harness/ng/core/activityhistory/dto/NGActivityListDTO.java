package io.harness.ng.core.activityhistory.dto;

import io.swagger.annotations.ApiModel;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
@ApiModel("ActivityList")
public class NGActivityListDTO {
  List<NGActivityDTO> activityHistoryForEntityUsage;
  List<ConnectivityCheckSummaryDTO> connectivityCheckSummaries;
}
