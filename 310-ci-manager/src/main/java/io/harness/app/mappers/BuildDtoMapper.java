package io.harness.app.mappers;

import static java.lang.String.format;

import io.harness.app.beans.dto.CIBuildResponseDTO;
import io.harness.app.beans.entities.CIBuildAuthor;
import io.harness.app.beans.entities.CIBuildBranchHook;
import io.harness.app.beans.entities.CIBuildCommit;
import io.harness.app.beans.entities.CIBuildPRHook;
import io.harness.app.beans.entities.CIBuildPipeline;
import io.harness.beans.execution.BranchWebhookEvent;
import io.harness.beans.execution.CommitDetails;
import io.harness.beans.execution.ExecutionSource;
import io.harness.beans.execution.PRWebhookEvent;
import io.harness.beans.execution.WebhookEvent;
import io.harness.beans.execution.WebhookExecutionSource;
import io.harness.beans.execution.WebhookGitUser;
import io.harness.ci.beans.entities.CIBuild;
import io.harness.ng.core.mapper.TagMapper;
import io.harness.ngpipeline.pipeline.beans.entities.NgPipelineEntity;

import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class BuildDtoMapper {
  private static final String PR_OPEN = "open";
  private static final String PR_CLOSED = "closed";
  private static final String PR_MERGED = "merged";
  private static final String PR = "pullRequest";
  private static final String BRANCH = "branch";

  public CIBuildResponseDTO writeBuildDto(CIBuild ciBuild, NgPipelineEntity ngPipelineEntity) throws InternalError {
    if (ngPipelineEntity == null) {
      throw new InternalError(
          format("pipeline:%s for build:%s", ciBuild.getPipelineIdentifier(), ciBuild.getBuildNumber()));
    }
    CIBuildResponseDTO ciBuildResponseDTO = CIBuildResponseDTO.builder()
                                                .id(ciBuild.getBuildNumber())
                                                .startTime(ciBuild.getCreatedAt())
                                                .pipeline(convertPipeline(ngPipelineEntity))
                                                .build();

    ExecutionSource executionSource = ciBuild.getExecutionSource();
    if (executionSource != null) {
      ciBuildResponseDTO.setTriggerType(executionSource.getType().toString().toLowerCase());
      if (executionSource.getType() == ExecutionSource.Type.WEBHOOK) {
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

  private CIBuildPipeline convertPipeline(NgPipelineEntity ngPipelineEntity) {
    return CIBuildPipeline.builder()
        .id(ngPipelineEntity.getIdentifier())
        .name(ngPipelineEntity.getNgPipeline().getName())
        .tags(TagMapper.convertToList(ngPipelineEntity.getNgPipeline().getTags()))
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
        .timeStamp(convertToMilliSeconds(commit.getTimeStamp()))
        .build();
  }

  private long convertToMilliSeconds(long seconds) {
    return seconds * 1000;
  }
}
