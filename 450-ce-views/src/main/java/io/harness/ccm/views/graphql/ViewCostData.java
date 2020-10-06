package io.harness.ccm.views.graphql;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ViewCostData {
  private double cost;
  private long minStartTime;
  private long maxStartTime;
}
