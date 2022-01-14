package io.harness.batch.processing.billing.timeseries.data;

import java.util.Set;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NodePodId {
  private String nodeId;
  private String clusterId;
  private Set<String> podId;
}
