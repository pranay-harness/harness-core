package io.harness.app.beans.entities;

import io.harness.ng.core.dashboard.AuthorInfo;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class BuildActiveInfo {
  private String piplineName;
  private String pipelineIdentifier;
  private String branch;
  private String commit;
  private String commitID;
  private String triggerType;
  private AuthorInfo author;
  private Long startTs;
  private String status;
  private Long endTs;
}
