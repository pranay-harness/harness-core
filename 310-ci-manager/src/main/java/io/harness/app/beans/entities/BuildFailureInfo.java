package io.harness.app.beans.entities;

import io.harness.ng.core.dashboard.AuthorInfo;
import io.harness.ng.core.dashboard.GitInfo;
import io.harness.ng.core.dashboard.ServiceDeploymentInfo;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class BuildFailureInfo {
  private String piplineName;
  private String pipelineIdentifier;
  private String branch;
  private String commit;
  private String commitID;
  private AuthorInfo author;
  private GitInfo gitInfo;
  private long startTs;
  private long endTs;
  private String triggerType;
  private String planExecutionId;
  private List<ServiceDeploymentInfo> serviceInfoList;
  String status;
}
