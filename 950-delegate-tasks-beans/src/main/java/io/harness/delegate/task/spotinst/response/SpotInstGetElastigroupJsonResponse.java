package io.harness.delegate.task.spotinst.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SpotInstGetElastigroupJsonResponse implements SpotInstTaskResponse {
  private String elastigroupJson;
}
