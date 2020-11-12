package io.harness.app.mappers;

import io.harness.beans.execution.BranchWebhookEvent;
import io.harness.beans.execution.CommitDetails;
import io.harness.beans.execution.PRWebhookEvent;
import io.harness.beans.execution.WebhookExecutionSource;
import io.harness.beans.execution.WebhookGitUser;
import io.harness.ci.beans.entities.CIBuild;
import io.harness.dto.OrchestrationGraphDTO;
import io.harness.ngpipeline.pipeline.beans.entities.NgPipelineEntity;
import io.harness.ngpipeline.pipeline.beans.yaml.NgPipeline;
import io.harness.pms.execution.Status;

import java.util.Arrays;

public class BuildDtoMapperTestHelper {
  public static final String ACCOUNT_ID = "ACCOUNT_ID";
  public static final String ORG_ID = "ORG_ID";
  public static final String PROJECT_ID = "PROJECT_ID";
  public static final String EXECUTION_ID = "executionId";
  public static final Long BUILD_ID = 4L;
  public static final String PIPELINE_ID = "123";
  public static final String PIPELINE_NAME = "test";

  public static final String BRANCH_NAME = "test";
  public static final String COMMIT_ID = "abcd";
  public static final Long PR_ID = 1L;
  public static final String PR_TITLE = "Pull request";
  public static final String USER_GIT_ID = "foo";

  public static CIBuild getBuild(WebhookExecutionSource executionSource) {
    return CIBuild.builder()
        .buildNumber(BUILD_ID)
        .accountIdentifier(ACCOUNT_ID)
        .orgIdentifier(ORG_ID)
        .projectIdentifier(PROJECT_ID)
        .pipelineIdentifier(PIPELINE_ID)
        .executionSource(executionSource)
        .executionId(EXECUTION_ID)
        .build();
  }

  public static CIBuild getPRBuild() {
    WebhookExecutionSource executionSource =
        WebhookExecutionSource.builder()
            .webhookEvent(PRWebhookEvent.builder().pullRequestId(PR_ID).title(PR_TITLE).build())
            .user(WebhookGitUser.builder().gitId(USER_GIT_ID).build())
            .build();
    return getBuild(executionSource);
  }

  public static CIBuild getBranchBuild() {
    WebhookExecutionSource executionSource =
        WebhookExecutionSource.builder()
            .webhookEvent(BranchWebhookEvent.builder()
                              .branchName(BRANCH_NAME)
                              .commitDetailsList(Arrays.asList(CommitDetails.builder().commitId(COMMIT_ID).build()))
                              .build())
            .user(WebhookGitUser.builder().gitId(USER_GIT_ID).build())
            .build();
    return getBuild(executionSource);
  }

  public static NgPipelineEntity getPipeline() {
    return NgPipelineEntity.builder()
        .identifier(PIPELINE_ID)
        .ngPipeline(NgPipeline.builder().name(PIPELINE_NAME).build())
        .build();
  }

  public static OrchestrationGraphDTO getGraph() {
    return OrchestrationGraphDTO.builder().planExecutionId(EXECUTION_ID).status(Status.SUCCEEDED).build();
  }
}
