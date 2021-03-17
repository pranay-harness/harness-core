package io.harness.pms.barriers.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel("BarrierExecutionInfo")
public class BarrierExecutionInfoDTO {
  String name;
  String identifier;
  long startedAt;
  boolean started;
  long timeoutIn;
  List<StageDetailDTO> stages;
}
