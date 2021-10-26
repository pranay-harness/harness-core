package io.harness.app.beans.entities;

import io.harness.ng.core.dashboard.AuthorInfo;

import io.harness.ng.core.dashboard.GitInfo;
import io.harness.ng.core.dashboard.ServiceDeploymentInfo;
import lombok.Builder;
import lombok.Value;

import java.util.List;

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
  private GitInfo gitInfo;
  private List<ServiceDeploymentInfo> serviceInfoList;
}
