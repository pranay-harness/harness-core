package io.harness.batch.processing.billing.timeseries.data;

import lombok.Builder;
import lombok.Data;

import java.util.Set;

@Data
@Builder
public class NodePodId {
  private String nodeId;
  private String clusterId;
  private Set<String> podId;
}
