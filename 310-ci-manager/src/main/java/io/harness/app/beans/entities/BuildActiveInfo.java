package io.harness.app.beans.entities;

import io.harness.ng.core.dashboard.AuthorInfo;
import io.harness.ng.core.dashboard.GitInfo;
import io.harness.ng.core.dashboard.ServiceDeploymentInfo;

import java.util.List;
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
  private String planExecutionId;
  private Long endTs;
  private GitInfo gitInfo;
  private List<ServiceDeploymentInfo> serviceInfoList;
}
