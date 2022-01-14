package io.harness.ng.core.activityhistory.dto;

import io.swagger.annotations.ApiModel;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@ApiModel("ActivityList")
public class NGActivityListDTO {
  List<NGActivityDTO> activityHistoriesForEntityUsage;
  ConnectivityCheckSummaryDTO connectivityCheckSummary;
}
