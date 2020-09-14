package io.harness.app.mappers;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.app.beans.dto.CIBuildResponseDTO;
import io.harness.app.beans.entities.CIBuildAuthor;
import io.harness.app.beans.entities.CIBuildBranchHook;
import io.harness.app.beans.entities.CIBuildCommit;
import io.harness.app.beans.entities.CIBuildPRHook;
import io.harness.app.beans.entities.CIBuildPipeline;
import io.harness.app.intfc.CIPipelineService;
import io.harness.beans.CIPipeline;
import io.harness.beans.execution.BranchWebhookEvent;
import io.harness.beans.execution.CommitDetails;
import io.harness.beans.execution.ExecutionSource;
import io.harness.beans.execution.PRWebhookEvent;
import io.harness.beans.execution.WebhookEvent;
import io.harness.beans.execution.WebhookExecutionSource;
import io.harness.beans.execution.WebhookGitUser;
import io.harness.ci.beans.entities.CIBuild;

import java.util.ArrayList;
import java.util.List;

@Singleton
public class BuildDtoMapper {
  private static final String PR_OPEN = "open";
  private static final String PR_CLOSED = "closed";
  private static final String PR_MERGED = "merged";
  private static final String PR = "pullRequest";
  private static final String BRANCH = "branch";

  @Inject private CIPipelineService ciPipelineService;

  public CIBuildResponseDTO writeBuildDto(CIBuild ciBuild, String accountId, String orgId, String projectId) {
    CIBuildResponseDTO ciBuildResponseDTO =
        CIBuildResponseDTO.builder()
            .id(ciBuild.getBuildNumber())
            .startTime(ciBuild.getCreatedAt())
            .pipeline(convertPipeline(ciBuild.getPipelineIdentifier(), accountId, orgId, projectId))
            .build();

    ExecutionSource executionSource = ciBuild.getExecutionSource();
    if (executionSource != null) {
      ciBuildResponseDTO.setTriggerType(executionSource.getType().toString().toLowerCase());
      if (executionSource.getType() == ExecutionSource.Type.Webhook) {
        WebhookExecutionSource webhookExecutionSource = (WebhookExecutionSource) executionSource;
        ciBuildResponseDTO.setAuthor(convertGitAuthor(webhookExecutionSource.getUser()));
        if (webhookExecutionSource.getWebhookEvent().getType() == WebhookEvent.Type.PR) {
          PRWebhookEvent prWebhookEvent = (PRWebhookEvent) webhookExecutionSource.getWebhookEvent();
          ciBuildResponseDTO.setPullRequest(convertPR(prWebhookEvent));
          ciBuildResponseDTO.setEvent(PR);
        } else if (webhookExecutionSource.getWebhookEvent().getType() == WebhookEvent.Type.BRANCH) {
          BranchWebhookEvent branchWebhookEvent = (BranchWebhookEvent) webhookExecutionSource.getWebhookEvent();
          ciBuildResponseDTO.setBranch(convertBranch(branchWebhookEvent));
          ciBuildResponseDTO.setEvent(BRANCH);
        }
      }
    }
    return ciBuildResponseDTO;
  }

  private CIBuildPRHook convertPR(PRWebhookEvent pr) {
    String state = PR_OPEN;
    if (pr.isClosed()) {
      state = PR_CLOSED;
    } else if (pr.isMerged()) {
      state = PR_MERGED;
    }
    return CIBuildPRHook.builder()
        .id(pr.getPullRequestId())
        .link(pr.getPullRequestLink())
        .title(pr.getTitle())
        .body(pr.getPullRequestBody())
        .sourceBranch(pr.getSourceBranch())
        .targetBranch(pr.getTargetBranch())
        .state(state)
        .build();
  }

  private CIBuildPipeline convertPipeline(String pipelineId, String accountId, String orgId, String projectId) {
    CIPipeline ciPipeline = ciPipelineService.readPipeline(pipelineId, accountId, orgId, projectId);
    return CIBuildPipeline.builder()
        .id(ciPipeline.getIdentifier())
        .name(ciPipeline.getName())
        .tags(ciPipeline.getTags())
        .build();
  }

  private CIBuildBranchHook convertBranch(BranchWebhookEvent branch) {
    List<CIBuildCommit> commitList = new ArrayList<>();
    branch.getCommitDetailsList().forEach(commit -> commitList.add(convertCommit(commit)));
    return CIBuildBranchHook.builder().name(branch.getBranchName()).link(branch.getLink()).commits(commitList).build();
  }

  private CIBuildAuthor convertGitAuthor(WebhookGitUser user) {
    return CIBuildAuthor.builder()
        .id(user.getGitId())
        .name(user.getName())
        .email(user.getEmail())
        .avatar(user.getAvatar())
        .build();
  }

  private CIBuildCommit convertCommit(CommitDetails commit) {
    return CIBuildCommit.builder()
        .id(commit.getCommitId())
        .message(commit.getMessage())
        .link(commit.getLink())
        .ownerEmail(commit.getOwnerEmail())
        .ownerId(commit.getOwnerId())
        .ownerName(commit.getOwnerName())
        .build();
  }
}
