package io.harness.spotinst.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class SpotInstListElastiGroupInstancesHealthResponse {
  private SpotInstRequest request;
  private ListElastiGroupInstancesHealthResponse response;
}