package ci.pipeline.execution;

import static io.harness.delegate.beans.connector.scm.GitAuthType.HTTP;
import static io.harness.delegate.beans.connector.scm.GitAuthType.SSH;
import static io.harness.govern.Switch.unhandled;

import io.harness.beans.DelegateTaskRequest;
import io.harness.beans.stages.IntegrationStageStepParameters;
import io.harness.delegate.beans.ci.pod.ConnectorDetails;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitHTTPAuthenticationDTO;
import io.harness.delegate.task.ci.CIBuildStatusPushParameters;
import io.harness.delegate.task.ci.GitSCMType;
import io.harness.exception.InvalidRequestException;
import io.harness.execution.NodeExecution;
import io.harness.git.GitClientHelper;
import io.harness.ng.core.NGAccess;
import io.harness.ngpipeline.common.AmbianceHelper;
import io.harness.ngpipeline.status.BuildStatusUpdateParameter;
import io.harness.plancreators.IntegrationStagePlanCreator;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.serializer.json.JsonOrchestrationUtils;
import io.harness.service.DelegateGrpcClientWrapper;
import io.harness.stateutils.buildstate.ConnectorUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
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

  public boolean shouldSendStatus(NodeExecution nodeExecution) {
    return Objects.equals(nodeExecution.getNode().getGroup(), IntegrationStagePlanCreator.GROUP_NAME);
  }

  public void sendStatusToGit(NodeExecution nodeExecution, Ambiance ambiance, String accountId) {
    IntegrationStageStepParameters integrationStageStepParameters = JsonOrchestrationUtils.asObject(
        nodeExecution.getResolvedStepParameters().toJson(), IntegrationStageStepParameters.class);
    BuildStatusUpdateParameter buildStatusUpdateParameter =
        integrationStageStepParameters.getBuildStatusUpdateParameter();

    if (buildStatusUpdateParameter != null) {
      CIBuildStatusPushParameters ciBuildStatusPushParameters =
          getCIBuildStatusPushParams(ambiance, buildStatusUpdateParameter, nodeExecution.getStatus());
      if (ciBuildStatusPushParameters.getState() != UNSUPPORTED) {
        DelegateTaskRequest delegateTaskRequest = DelegateTaskRequest.builder()
                                                      .accountId(accountId)
                                                      .taskSetupAbstractions(ambiance.getSetupAbstractions())
                                                      .executionTimeout(java.time.Duration.ofSeconds(60))
                                                      .taskType("BUILD_STATUS")
                                                      .taskParameters(ciBuildStatusPushParameters)
                                                      .taskDescription("CI git build status task")
                                                      .build();

        String taskId = delegateGrpcClientWrapper.submitAsyncTask(delegateTaskRequest);
        log.info("Submitted git status update request for stage {}, planId {}, commitId {}, status {} with taskId {}",
            buildStatusUpdateParameter.getIdentifier(), nodeExecution.getStatus().name(),
            buildStatusUpdateParameter.getSha(), buildStatusUpdateParameter.getState(), taskId);
      } else {
        log.info("Skipping git status update request for stage {}, planId {}, commitId {}, status {}, scm type",
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

    GitConfigDTO gitConfigDTO = (GitConfigDTO) gitConnector.getConnectorConfig();
    String userName = "";
    if (gitConfigDTO.getGitAuthType() == HTTP) {
      GitHTTPAuthenticationDTO gitAuth = (GitHTTPAuthenticationDTO) gitConfigDTO.getGitAuth();
      userName = gitAuth.getUsername();
    } else if (gitConfigDTO.getGitAuthType() == SSH) {
      // TODO Require UserName in git ssh DTO
    }

    GitSCMType gitSCMType = retrieveSCMType(gitConfigDTO.getUrl());
    CIBuildStatusPushParameters ciBuildPushStatusParameters =
        CIBuildStatusPushParameters.builder()
            .desc(generateDesc(buildStatusUpdateParameter.getIdentifier(), status.name()))
            .sha(buildStatusUpdateParameter.getSha())
            .gitSCMType(gitSCMType)
            .token(retrieveAuthToken(gitSCMType))
            .userName(userName)
            .owner(gitClientHelper.getGitOwner(gitConfigDTO.getUrl()))
            .repo(gitClientHelper.getGitRepo(gitConfigDTO.getUrl()))
            .identifier(buildStatusUpdateParameter.getIdentifier())
            .state(retrieveBuildStatusState(gitSCMType, status))
            .build();

    ciBuildPushStatusParameters.setKey("-----BEGIN PRIVATE KEY-----\n"
        + "MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQC18V6lyGHt6q9u\n"
        + "GSZV94j0Flt6d9/AQmVACyGHKu25IO6v0pRB3YA7zHllc+ZViI+VQpfvkIFUakcO\n"
        + "7nneDsczQ4tFYWcZx5CMxwV4RF+FGBrKOspsKqsQ33IL7CyF40vOtnf7NZRMl0Lh\n"
        + "MIg3FXhCk/4X2MNosCFUyEUGJKJwepXTH9tgAMLpwc5InnOSRpG8+3lWzGREOUDd\n"
        + "ZyGnkEG/gG5aiHyjpp0xu1ixiXsRp8cSq4WveAzT7kwioTDOY91jStx1wun58goT\n"
        + "nEaocIercyQApEcdfvgWEN04+xiO4LUCnUrLpomxkwweVFZ9qOH6KsSHo6ZIfsp2\n"
        + "kgo12x5LAgMBAAECggEAQ8+kQRHAPhZcMCK7gQrzRlYW3jxTbqrQZeBALMq5M2is\n"
        + "zWckzq+pnaAGFuPtky+EpFLfofAv47CAr3X+gd7sK5UfEUrOTHNu0qlSxpJlL4ve\n"
        + "YEUtMMduXqmJLhxmM7iVhoPHkB9WGH2/9YJLIoyj99yEtYqauif7JEhIQZPh3x2P\n"
        + "lHEdsa395BIeTHVyKuYIh44bbt4mmmp29YXXCyk2rc70gAdSVUOYf+IyIS4pVxv8\n"
        + "pkOLqQhH+bkIa/eXR4DTr5mP1oFtrbYFj9miFaNZozRQs+HeiRcZT4oI7Nm7opoC\n"
        + "iJgriDI/QtEG0wmVyJXNw08KcMQDbrTTPWiEQkZv4QKBgQDuPASsfGUV8X0RS+Gw\n"
        + "8sf7gIK194x+y0J3TlcHtqlbxC7lLizawB8Hhp5sFJc37eOnXfom5FkE7E5/Cf+i\n"
        + "BXFsDpzqK28Z3UlNMYVCSk26VOg6u1hXhrvkWh+eY3fedqwiTAoqBIM+wmMabN1Z\n"
        + "d+1V4k4evJaDzpQnZmpcADr1AwKBgQDDgrl7M2PW3ufACqMo88xxaV01r0NcS8gC\n"
        + "Iikk5K3H1CVN58ulR0OcFog+7zy7ca+rDcFX9DoU0R9JPUVw3JuLt2+Xsez5mnNr\n"
        + "g0pfc7tjBEUnMC7A4IytqsLPtYTQnSqaxuVrsIs+zrx45IsKKc+v951zt/4lMZOV\n"
        + "2uUcamC7GQKBgG6tpYI43IGgSnlxpm2drTjz0EYXtsblSYDB/X7Q5seCUkMY+6+5\n"
        + "F/FYIluWCVbrhxsnduMArTazThiJHaE5JCOOemn0Oc5rVvWs7vsIKCpL7gPzK6ym\n"
        + "JL6G/C+KiJLq0Tex2fsBU7QhfQc20nMRW0rOM3rmJIshuwS++OS7GqjfAoGALvuK\n"
        + "IS4fTvJwFLk5rkywE4zzZkRA1rwrSz/0TTZbAItdj5QlXwl6GNddVGpfWNggE+YR\n"
        + "UVaSYpBCiXIc7ttE0dV6DqUmQnE4TVzWkYuZO1k6WQl+IsGTbOR9PjbrvMoYA+vK\n"
        + "FA/v1l8N8atSMlYL38iMYNOVUlDQm5Fnv2Vc63kCgYBp0tdIpLCJtSzZ7w1VNk6X\n"
        + "5UeQWqNGZoXPc4rsX8xe3HBO4Fuw7hmXL2KgfLbTRSpfV32zSkdmWHUlMoZAHaMt\n"
        + "cPRXcs2AlHrsOiF7RhiUT/2Nz+WNyzCIovDP7a0bAaMB1S2rlfuyOnFs4CcPLJDL\n"
        + "3N+Xjw5xLWBb1oDXF3UKuA==\n"
        + "-----END PRIVATE KEY-----");

    ciBuildPushStatusParameters.setInstallId("13076271");
    ciBuildPushStatusParameters.setAppId("84820");

    return ciBuildPushStatusParameters;
  }

  private String generateDesc(String identifier, String status) {
    return String.format("Execution status of stage %s:  %s  ", identifier, status);
  }

  // TODO Change it via proper grouping
  private GitSCMType retrieveSCMType(String url) {
    String scmType = gitClientHelper.getGitSCM(url);

    if (url.contains("github")) {
      return GitSCMType.GITHUB;
    } else if (url.contains("bitbucket")) {
      return GitSCMType.BITBUCKET;
    } else if (url.contains("gitlab")) {
      return GitSCMType.GITLAB;
    } else {
      throw new InvalidRequestException("scmType " + scmType + "is not supported");
    }
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

  private String retrieveAuthToken(GitSCMType gitSCMType) {
    switch (gitSCMType) {
      case GITHUB:
        return ""; // It does not require token because auth occur via github app
      case GITLAB:
        return "p_wmFaUMEL8zZAe8hcM_";
      case BITBUCKET:
        return "c9yZThYsTMQFHXWDjD8j";
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

    return UNSUPPORTED;
  }

  private ConnectorDetails getGitConnector(NGAccess ngAccess, String connectorRef) {
    return connectorUtils.getConnectorDetails(ngAccess, connectorRef);
  }
}
