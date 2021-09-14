/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.spotinst.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ElastiGroup implements Cloneable {
  private String id;
  private String name;
  private ElastiGroupCapacity capacity;

  @Override
  public ElastiGroup clone() {
    return ElastiGroup.builder()
        .id(id)
        .name(name)
        .capacity(ElastiGroupCapacity.builder()
                      .target(capacity.getTarget())
                      .minimum(capacity.getMinimum())
                      .maximum(capacity.getMaximum())
                      .build())
        .build();
  }
}
