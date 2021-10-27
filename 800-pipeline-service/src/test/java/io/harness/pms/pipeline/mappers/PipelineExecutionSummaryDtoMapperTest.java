package io.harness.pms.pipeline.mappers;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.rule.OwnerRule.NAMAN;
import static io.harness.rule.OwnerRule.PRASHANTSHARMA;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

import io.harness.PipelineServiceTestBase;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.engine.executions.retry.RetryExecutionMetadata;
import io.harness.execution.StagesExecutionMetadata;
import io.harness.gitsync.sdk.EntityGitDetails;
import io.harness.pms.pipeline.PipelineEntity;
import io.harness.pms.pipeline.service.PMSPipelineService;
import io.harness.pms.plan.execution.beans.PipelineExecutionSummaryEntity;
import io.harness.pms.plan.execution.beans.dto.PipelineExecutionSummaryDTO;
import io.harness.rule.Owner;

import java.util.Collections;
import java.util.Optional;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@OwnedBy(PIPELINE)
public class PipelineExecutionSummaryDtoMapperTest extends PipelineServiceTestBase {
  @InjectMocks PipelineExecutionSummaryDtoMapper pipelineExecutionSummaryDtoMapper;
  @Mock PMSPipelineService pipelineService;

  String accountId = "acc";
  String orgId = "org";
  String projId = "proj";
  String pipelineId = "pipelineId";
  String planId = "plan-random";

  String branch = "branch";
  String repo = "repo";
  String objectId = "o";
  String rootFolder = "folder/.harness/";
  String file = "file.yaml";

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testToDtoForGitDetails() {
    PipelineExecutionSummaryEntity executionSummaryEntity = PipelineExecutionSummaryEntity.builder()
                                                                .accountId(accountId)
                                                                .orgIdentifier(orgId)
                                                                .projectIdentifier(projId)
                                                                .pipelineIdentifier(pipelineId)
                                                                .runSequence(1)
                                                                .planExecutionId(planId)
                                                                .build();
    PipelineExecutionSummaryDTO executionSummaryDTO =
        pipelineExecutionSummaryDtoMapper.toDto(executionSummaryEntity, null);
    assertThat(executionSummaryDTO).isNotNull();
    assertThat(executionSummaryDTO.getGitDetails()).isNull();

    EntityGitDetails entityGitDetails =
        EntityGitDetails.builder().branch(branch).repoIdentifier(repo).objectId(objectId).build();
    executionSummaryDTO = pipelineExecutionSummaryDtoMapper.toDto(executionSummaryEntity, entityGitDetails);
    assertThat(executionSummaryDTO).isNotNull();
    assertThat(executionSummaryDTO.getGitDetails()).isNotNull();
    assertThat(executionSummaryDTO.getGitDetails().getBranch()).isEqualTo(branch);
    assertThat(executionSummaryDTO.getGitDetails().getRepoIdentifier()).isEqualTo(repo);
    assertThat(executionSummaryDTO.getGitDetails().getObjectId()).isEqualTo(objectId);
    assertThat(executionSummaryDTO.getGitDetails().getFilePath()).isNull();
    assertThat(executionSummaryDTO.getGitDetails().getRootFolder()).isNull();

    entityGitDetails = EntityGitDetails.builder()
                           .branch(branch)
                           .repoIdentifier(repo)
                           .objectId(objectId)
                           .rootFolder("__default__")
                           .filePath("__default__")
                           .build();
    executionSummaryDTO = pipelineExecutionSummaryDtoMapper.toDto(executionSummaryEntity, entityGitDetails);
    assertThat(executionSummaryDTO).isNotNull();
    assertThat(executionSummaryDTO.getGitDetails()).isNotNull();
    assertThat(executionSummaryDTO.getGitDetails().getBranch()).isEqualTo(branch);
    assertThat(executionSummaryDTO.getGitDetails().getRepoIdentifier()).isEqualTo(repo);
    assertThat(executionSummaryDTO.getGitDetails().getObjectId()).isEqualTo(objectId);
    assertThat(executionSummaryDTO.getGitDetails().getFilePath()).isNull();
    assertThat(executionSummaryDTO.getGitDetails().getRootFolder()).isNull();

    entityGitDetails = EntityGitDetails.builder()
                           .branch(branch)
                           .repoIdentifier(repo)
                           .objectId(objectId)
                           .rootFolder(rootFolder)
                           .filePath(file)
                           .build();
    executionSummaryDTO = pipelineExecutionSummaryDtoMapper.toDto(executionSummaryEntity, entityGitDetails);
    assertThat(executionSummaryDTO).isNotNull();
    assertThat(executionSummaryDTO.getGitDetails()).isNotNull();
    assertThat(executionSummaryDTO.getGitDetails().getBranch()).isEqualTo(branch);
    assertThat(executionSummaryDTO.getGitDetails().getRepoIdentifier()).isEqualTo(repo);
    assertThat(executionSummaryDTO.getGitDetails().getObjectId()).isEqualTo(objectId);
    assertThat(executionSummaryDTO.getGitDetails().getFilePath()).isEqualTo(file);
    assertThat(executionSummaryDTO.getGitDetails().getRootFolder()).isEqualTo(rootFolder);
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testToDtoForStagesExecutionMetadata() {
    doReturn(Optional.of(PipelineEntity.builder().yaml(getPipelineYaml()).build()))
        .when(pipelineService)
        .get(accountId, orgId, projId, pipelineId, false);

    PipelineExecutionSummaryEntity executionSummaryEntity = PipelineExecutionSummaryEntity.builder()
                                                                .accountId(accountId)
                                                                .orgIdentifier(orgId)
                                                                .projectIdentifier(projId)
                                                                .pipelineIdentifier(pipelineId)
                                                                .runSequence(1)
                                                                .planExecutionId(planId)
                                                                .build();
    PipelineExecutionSummaryDTO executionSummaryDTO =
        pipelineExecutionSummaryDtoMapper.toDto(executionSummaryEntity, null);
    assertThat(executionSummaryDTO.isStagesExecution()).isFalse();
    assertThat(executionSummaryDTO.getStagesExecuted()).isNull();

    PipelineExecutionSummaryEntity executionSummaryEntityWithStages =
        PipelineExecutionSummaryEntity.builder()
            .accountId(accountId)
            .orgIdentifier(orgId)
            .projectIdentifier(projId)
            .pipelineIdentifier(pipelineId)
            .runSequence(1)
            .planExecutionId(planId)
            .stagesExecutionMetadata(StagesExecutionMetadata.builder()
                                         .isStagesExecution(true)
                                         .stageIdentifiers(Collections.singletonList("s1"))
                                         .build())
            .build();
    PipelineExecutionSummaryDTO executionSummaryDTOWithStages =
        pipelineExecutionSummaryDtoMapper.toDto(executionSummaryEntityWithStages, null);
    assertThat(executionSummaryDTOWithStages.isStagesExecution()).isTrue();
    assertThat(executionSummaryDTOWithStages.getStagesExecuted()).hasSize(1);
    assertThat(executionSummaryDTOWithStages.getStagesExecuted().contains("s1")).isTrue();
    assertThat(executionSummaryDTOWithStages.getStagesExecutedNames()).hasSize(1);
    assertThat(executionSummaryDTOWithStages.getStagesExecutedNames().get("s1")).isEqualTo("s one");
  }

  private String getPipelineYaml() {
    return "pipeline:\n"
        + "  identifier: p1\n"
        + "  stages:\n"
        + "  - stage:\n"
        + "      identifier: s1\n"
        + "      name: s one\n"
        + "  - stage:\n"
        + "      identifier: s2\n"
        + "      name: s two";
  }

  @Test
  @Owner(developers = PRASHANTSHARMA)
  @Category(UnitTests.class)
  public void testToDtoForRetryHistory() {
    PipelineExecutionSummaryEntity executionSummaryEntity = PipelineExecutionSummaryEntity.builder()
                                                                .accountId(accountId)
                                                                .orgIdentifier(orgId)
                                                                .projectIdentifier(projId)
                                                                .pipelineIdentifier(pipelineId)
                                                                .runSequence(1)
                                                                .planExecutionId(planId)
                                                                .build();

    // when isLatest is notSet (default value is true)
    PipelineExecutionSummaryDTO executionSummaryDTO =
        pipelineExecutionSummaryDtoMapper.toDto(executionSummaryEntity, null);
    assertThat(executionSummaryDTO.isCanRetry()).isEqualTo(true);
    assertThat(executionSummaryDTO.isShowRetryHistory()).isEqualTo(false);

    // added rootParentId and setting isLatest false
    PipelineExecutionSummaryEntity executionSummaryEntityWithRootParentId =
        PipelineExecutionSummaryEntity.builder()
            .accountId(accountId)
            .orgIdentifier(orgId)
            .projectIdentifier(projId)
            .pipelineIdentifier(pipelineId)
            .runSequence(1)
            .retryExecutionMetadata(RetryExecutionMetadata.builder().rootExecutionId("rootParentId").build())
            .isLatestExecution(false)
            .planExecutionId(planId)
            .build();
    executionSummaryDTO = pipelineExecutionSummaryDtoMapper.toDto(executionSummaryEntityWithRootParentId, null);
    assertThat(executionSummaryDTO.isCanRetry()).isEqualTo(false);
    assertThat(executionSummaryDTO.isShowRetryHistory()).isEqualTo(true);

    // isLatestTrue
    PipelineExecutionSummaryEntity executionSummaryEntityWithIsLatest =
        PipelineExecutionSummaryEntity.builder()
            .accountId(accountId)
            .orgIdentifier(orgId)
            .projectIdentifier(projId)
            .pipelineIdentifier(pipelineId)
            .runSequence(1)
            .isLatestExecution(true)
            .retryExecutionMetadata(RetryExecutionMetadata.builder().rootExecutionId("rootParentId").build())
            .planExecutionId(planId)
            .build();
    executionSummaryDTO = pipelineExecutionSummaryDtoMapper.toDto(executionSummaryEntityWithIsLatest, null);
    assertThat(executionSummaryDTO.isCanRetry()).isEqualTo(true);
    assertThat(executionSummaryDTO.isShowRetryHistory()).isEqualTo(true);
  }
}