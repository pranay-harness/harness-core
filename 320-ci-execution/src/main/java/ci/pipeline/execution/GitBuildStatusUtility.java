package ci.pipeline.execution;

import static io.harness.delegate.beans.connector.ConnectorType.BITBUCKET;
import static io.harness.delegate.beans.connector.ConnectorType.GITHUB;
import static io.harness.delegate.beans.connector.ConnectorType.GITLAB;
import static io.harness.delegate.beans.connector.scm.GitConnectionType.ACCOUNT;
import static io.harness.govern.Switch.unhandled;
import static io.harness.pms.execution.utils.StatusUtils.isFinalStatus;

import io.harness.PipelineUtils;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DelegateTaskRequest;
import io.harness.beans.stages.IntegrationStageStepParametersPMS;
import io.harness.delegate.beans.ci.pod.ConnectorDetails;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketConnectorDTO;
import io.harness.delegate.beans.connector.scm.github.GithubConnectorDTO;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabConnectorDTO;
import io.harness.delegate.task.ci.CIBuildStatusPushParameters;
import io.harness.delegate.task.ci.GitSCMType;
import io.harness.exception.ngexception.CIStageExecutionException;
import io.harness.execution.NodeExecution;
import io.harness.git.GitClientHelper;
import io.harness.ng.core.NGAccess;
import io.harness.ngpipeline.common.AmbianceHelper;
import io.harness.ngpipeline.status.BuildStatusUpdateParameter;
import io.harness.plancreator.steps.common.StageElementParameters;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;
import io.harness.service.DelegateGrpcClientWrapper;
import io.harness.stateutils.buildstate.ConnectorUtils;
import io.harness.steps.StepOutcomeGroup;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.time.Duration;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@OwnedBy(HarnessTeam.CI)
public class GitBuildStatusUtility {
  private static final String UNSUPPORTED = "UNSUPPORTED";
  private static final String GITHUB_ERROR = "error";
  private static final String GITHUB_SUCCESS = "success";
  private static final String GITHUB_FAILED = "failure";
  private static final String GITHUB_PENDING = "pending";
  private static final String BITBUCKET_FAILED = "FAILED";
  private static final String BITBUCKET_SUCCESS = "SUCCESSFUL";
  private static final String BITBUCKET_STOPPED = "STOPPED";
  private static final String BITBUCKET_PENDING = "INPROGRESS";
  private static final String GITLAB_FAILED = "failed";
  private static final String GITLAB_CANCELED = "canceled";
  private static final String GITLAB_PENDING = "pending";
  private static final String GITLAB_SUCCESS = "success";

  @Inject GitClientHelper gitClientHelper;
  @Inject private ConnectorUtils connectorUtils;
  @Inject private DelegateGrpcClientWrapper delegateGrpcClientWrapper;
  @Inject @Named("ngBaseUrl") private String ngBaseUrl;
  @Inject private PipelineUtils pipelineUtils;

  public boolean shouldSendStatus(NodeExecution nodeExecution) {
    return Objects.equals(nodeExecution.getNode().getGroup(), StepOutcomeGroup.STAGE.name());
  }

  public void sendStatusToGit(NodeExecution nodeExecution, Ambiance ambiance, String accountId) {
    StageElementParameters stageElementParameters =
        RecastOrchestrationUtils.fromDocument(nodeExecution.getResolvedStepParameters(), StageElementParameters.class);

    IntegrationStageStepParametersPMS integrationStageStepParameters =
        (IntegrationStageStepParametersPMS) stageElementParameters.getSpec();
    BuildStatusUpdateParameter buildStatusUpdateParameter =
        integrationStageStepParameters.getBuildStatusUpdateParameter();

    if (buildStatusUpdateParameter != null) {
      CIBuildStatusPushParameters ciBuildStatusPushParameters =
          getCIBuildStatusPushParams(ambiance, buildStatusUpdateParameter, nodeExecution.getStatus());

      /* This check is require because delegate is not honouring the ordering and
         there are instances where we are overriding final status with prev state status i.e running specially in case
         time interval is minuscule
      */

      if (isFinalStatus(nodeExecution.getStatus())) {
        try {
          Thread.sleep(5000);
        } catch (InterruptedException exception) {
          // Ignore
        }
      }

      if (ciBuildStatusPushParameters.getState() != UNSUPPORTED) {
        DelegateTaskRequest delegateTaskRequest = DelegateTaskRequest.builder()
                                                      .accountId(accountId)
                                                      .taskSetupAbstractions(ambiance.getSetupAbstractions())
                                                      .executionTimeout(java.time.Duration.ofSeconds(60))
                                                      .taskType("BUILD_STATUS")
                                                      .taskParameters(ciBuildStatusPushParameters)
                                                      .taskDescription("CI git build status task")
                                                      .build();

        String taskId = delegateGrpcClientWrapper.submitAsyncTask(delegateTaskRequest, Duration.ZERO);
        log.info("Submitted git status update request for stage {}, planId {}, commitId {}, status {} with taskId {}",
            buildStatusUpdateParameter.getIdentifier(), nodeExecution.getStatus().name(),
            buildStatusUpdateParameter.getSha(), buildStatusUpdateParameter.getState(), taskId);
      } else {
        log.info("Skipping git status update request for stage {}, planId {}, commitId {}, status {}, scm type {}",
            buildStatusUpdateParameter.getIdentifier(), nodeExecution.getStatus().name(),
            buildStatusUpdateParameter.getSha(), buildStatusUpdateParameter.getState(),
            ciBuildStatusPushParameters.getGitSCMType());
      }
    }
  }

  private CIBuildStatusPushParameters getCIBuildStatusPushParams(
      Ambiance ambiance, BuildStatusUpdateParameter buildStatusUpdateParameter, Status status) {
    NGAccess ngAccess = AmbianceHelper.getNgAccess(ambiance);
    ConnectorDetails gitConnector = getGitConnector(ngAccess, buildStatusUpdateParameter.getConnectorIdentifier());

    boolean isAccountLevelConnector = isAccountLevelConnector(gitConnector, buildStatusUpdateParameter.getRepoName());

    String repoName = buildStatusUpdateParameter.getRepoName();
    if (!isAccountLevelConnector) {
      repoName = gitClientHelper.getGitRepo(retrieveURL(gitConnector));
    }

    GitSCMType gitSCMType = retrieveSCMType(gitConnector);
    return CIBuildStatusPushParameters.builder()
        .detailsUrl(getBuildDetailsUrl(
            ngAccess, ambiance.getMetadata().getPipelineIdentifier(), ambiance.getMetadata().getExecutionUuid()))
        .desc(generateDesc(
            buildStatusUpdateParameter.getIdentifier(), buildStatusUpdateParameter.getName(), status.name()))
        .sha(buildStatusUpdateParameter.getSha())
        .gitSCMType(gitSCMType)
        .connectorDetails(gitConnector)
        .userName(connectorUtils.fetchUserName(gitConnector))
        .owner(gitClientHelper.getGitOwner(retrieveURL(gitConnector)))
        .repo(repoName)
        .identifier(buildStatusUpdateParameter.getIdentifier())
        .state(retrieveBuildStatusState(gitSCMType, status))
        .build();
  }

  private String generateDesc(String identifier, String name, String status) {
    return String.format("Execution status of stage  [%s (%s)]: %s ", name, identifier, status);
  }

  private GitSCMType retrieveSCMType(ConnectorDetails gitConnector) {
    if (gitConnector.getConnectorType() == GITHUB) {
      return GitSCMType.GITHUB;
    } else if (gitConnector.getConnectorType() == BITBUCKET) {
      return GitSCMType.BITBUCKET;
    } else if (gitConnector.getConnectorType() == GITLAB) {
      return GitSCMType.GITLAB;
    } else {
      throw new CIStageExecutionException("scmType " + gitConnector.getConnectorType() + "is not supported");
    }
  }

  private String retrieveURL(ConnectorDetails gitConnector) {
    if (gitConnector.getConnectorType() == GITHUB) {
      GithubConnectorDTO gitConfigDTO = (GithubConnectorDTO) gitConnector.getConnectorConfig();
      return gitConfigDTO.getUrl();
    } else if (gitConnector.getConnectorType() == BITBUCKET) {
      BitbucketConnectorDTO gitConfigDTO = (BitbucketConnectorDTO) gitConnector.getConnectorConfig();
      return gitConfigDTO.getUrl();
    } else if (gitConnector.getConnectorType() == GITLAB) {
      GitlabConnectorDTO gitConfigDTO = (GitlabConnectorDTO) gitConnector.getConnectorConfig();
      return gitConfigDTO.getUrl();
    } else {
      throw new CIStageExecutionException("scmType " + gitConnector.getConnectorType() + "is not supported");
    }
  }

  private boolean isAccountLevelConnector(ConnectorDetails gitConnector, String repoName) {
    if (gitConnector.getConnectorType() == GITHUB) {
      GithubConnectorDTO gitConfigDTO = (GithubConnectorDTO) gitConnector.getConnectorConfig();
      if (gitConfigDTO.getConnectionType() == ACCOUNT) {
        return true;
      }
    } else if (gitConnector.getConnectorType() == BITBUCKET) {
      BitbucketConnectorDTO gitConfigDTO = (BitbucketConnectorDTO) gitConnector.getConnectorConfig();
      if (gitConfigDTO.getConnectionType() == ACCOUNT) {
        return true;
      }
    } else if (gitConnector.getConnectorType() == GITLAB) {
      GitlabConnectorDTO gitConfigDTO = (GitlabConnectorDTO) gitConnector.getConnectorConfig();
      if (gitConfigDTO.getConnectionType() == ACCOUNT) {
        return true;
      }
    } else {
      throw new CIStageExecutionException("scmType " + gitConnector.getConnectorType() + "is not supported");
    }

    return false;
  }

  private String retrieveBuildStatusState(GitSCMType gitSCMType, Status status) {
    switch (gitSCMType) {
      case GITHUB:
        return getGitHubStatus(status);
      case GITLAB:
        return getGitLabStatus(status);
      case BITBUCKET:
        return getBitBucketStatus(status);
      default:
        unhandled(gitSCMType);
        return UNSUPPORTED;
    }
  }

  private String getGitHubStatus(Status status) {
    if (status == Status.ERRORED) {
      return GITHUB_ERROR;
    }
    if (status == Status.ABORTED || status == Status.FAILED) {
      return GITHUB_FAILED;
    }
    if (status == Status.SUCCEEDED) {
      return GITHUB_SUCCESS;
    }
    if (status == Status.RUNNING) {
      return GITHUB_PENDING;
    }
    if (status == Status.QUEUED) {
      return GITHUB_PENDING;
    }

    return UNSUPPORTED;
  }

  private String getGitLabStatus(Status status) {
    if (status == Status.ERRORED || status == Status.FAILED) {
      return GITLAB_FAILED;
    }
    if (status == Status.ABORTED) {
      return GITLAB_CANCELED;
    }
    if (status == Status.SUCCEEDED) {
      return GITLAB_SUCCESS;
    }
    if (status == Status.RUNNING) {
      return GITLAB_PENDING;
    }
    if (status == Status.QUEUED) {
      return GITLAB_PENDING;
    }

    return UNSUPPORTED;
  }

  private String getBitBucketStatus(Status status) {
    if (status == Status.ERRORED) {
      return BITBUCKET_FAILED;
    }
    if (status == Status.ABORTED || status == Status.FAILED) {
      return BITBUCKET_STOPPED;
    }
    if (status == Status.SUCCEEDED) {
      return BITBUCKET_SUCCESS;
    }
    if (status == Status.RUNNING) {
      return BITBUCKET_PENDING;
    }
    if (status == Status.QUEUED) {
      return BITBUCKET_PENDING;
    }

    return UNSUPPORTED;
  }

  private ConnectorDetails getGitConnector(NGAccess ngAccess, String connectorRef) {
    return connectorUtils.getConnectorDetails(ngAccess, connectorRef);
  }

  private String getBuildDetailsUrl(NGAccess ngAccess, String pipelineId, String executionId) {
    return pipelineUtils.getBuildDetailsUrl(ngAccess, pipelineId, executionId, ngBaseUrl);
  }
}
