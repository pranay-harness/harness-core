package io.harness.spotinst.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ListElastiGroupsResponse {
  private ResponseStatus status;
  private String kind;
  private List<ElastiGroup> items;
  int count;
}
