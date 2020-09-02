package io.harness.ccm.cluster.entities;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class K8sLabelFilter {
  private String accountId;
  private String labelName;
  private String searchString;
  private long startTime;
  private long endTime;
  private int limit;
  private int offset;
}
