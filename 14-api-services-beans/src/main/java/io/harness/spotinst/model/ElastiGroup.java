package io.harness.spotinst.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ElastiGroup {
  private String id;
  private String name;
  private ElastiGroupCapacity capacity;

  public ElastiGroup clone() {
    return ElastiGroup.builder().id(id).name(name).capacity(capacity).build();
  }
}