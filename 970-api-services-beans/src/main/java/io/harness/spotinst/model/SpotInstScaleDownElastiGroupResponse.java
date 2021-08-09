package io.harness.spotinst.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class SpotInstScaleDownElastiGroupResponse {
  private SpotInstRequest request;
  private ScaleDownElastiGroupResponse response;
}
