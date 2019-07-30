package io.harness.spotinst.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ElastiGroupCapacity {
  private int minimum;
  private int maximum;
  private int target;
  private String unit;
}
