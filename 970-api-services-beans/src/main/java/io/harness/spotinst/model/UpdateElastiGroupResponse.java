package io.harness.spotinst.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateElastiGroupResponse {
  private ResponseStatus status;
  private List<ResponseStatus> errors;
}
