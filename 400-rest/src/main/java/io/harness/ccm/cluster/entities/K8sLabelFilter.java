package io.harness.ccm.cluster.entities;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(CE)
public class K8sLabelFilter {
  private String accountId;
  private String labelName;
  private String searchString;
  private long startTime;
  private long endTime;
  private int limit;
  private int offset;
}
