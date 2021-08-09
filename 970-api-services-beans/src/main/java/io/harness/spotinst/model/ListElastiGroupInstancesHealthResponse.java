package io.harness.spotinst.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ListElastiGroupInstancesHealthResponse {
  private ResponseStatus status;
  private String kind;
  private List<ElastiGroupInstanceHealth> items;
  int count;
}
