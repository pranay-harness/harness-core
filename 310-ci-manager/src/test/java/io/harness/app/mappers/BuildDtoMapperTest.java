package io.harness.app.mappers;

import static io.harness.app.mappers.BuildDtoMapperTestHelper.BRANCH_NAME;
import static io.harness.app.mappers.BuildDtoMapperTestHelper.BUILD_ID;
import static io.harness.app.mappers.BuildDtoMapperTestHelper.COMMIT_ID;
import static io.harness.app.mappers.BuildDtoMapperTestHelper.PIPELINE_ID;
import static io.harness.app.mappers.BuildDtoMapperTestHelper.PIPELINE_NAME;
import static io.harness.app.mappers.BuildDtoMapperTestHelper.PR_ID;
import static io.harness.app.mappers.BuildDtoMapperTestHelper.PR_TITLE;
import static io.harness.app.mappers.BuildDtoMapperTestHelper.getBranchBuild;
import static io.harness.app.mappers.BuildDtoMapperTestHelper.getPRBuild;
import static io.harness.app.mappers.BuildDtoMapperTestHelper.getPipeline;
import static io.harness.rule.OwnerRule.SHUBHAM;

import static junit.framework.TestCase.assertEquals;

import io.harness.app.beans.dto.CIBuildResponseDTO;
import io.harness.app.impl.CIManagerTestBase;
import io.harness.category.element.UnitTests;
import io.harness.ci.beans.entities.CIBuild;
import io.harness.rule.Owner;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;

@Slf4j
public class BuildDtoMapperTest extends CIManagerTestBase {
  @InjectMocks BuildDtoMapper buildDtoMapper;

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void writePRBuildDto() {
    CIBuild ciBuild = getPRBuild();

    CIBuildResponseDTO responseDTO = buildDtoMapper.writeBuildDto(ciBuild, getPipeline());
    log.info("Response: {}", responseDTO);
    assertEquals(responseDTO.getId(), BUILD_ID);
    assertEquals(responseDTO.getTriggerType(), "webhook");
    assertEquals(responseDTO.getEvent(), "pullRequest");
    assertEquals(responseDTO.getPipeline().getName(), PIPELINE_NAME);
    assertEquals(responseDTO.getPipeline().getId(), PIPELINE_ID);
    assertEquals(responseDTO.getPullRequest().getId(), PR_ID);
    assertEquals(responseDTO.getPullRequest().getTitle(), PR_TITLE);
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void writeBranchBuildDto() {
    CIBuild ciBuild = getBranchBuild();

    CIBuildResponseDTO responseDTO = buildDtoMapper.writeBuildDto(ciBuild, getPipeline());
    log.info("Response: {}", responseDTO);
    assertEquals(responseDTO.getId(), BUILD_ID);
    assertEquals(responseDTO.getTriggerType(), "webhook");
    assertEquals(responseDTO.getEvent(), "branch");
    assertEquals(responseDTO.getPipeline().getName(), PIPELINE_NAME);
    assertEquals(responseDTO.getPipeline().getId(), PIPELINE_ID);
    assertEquals(responseDTO.getBranch().getName(), BRANCH_NAME);
    assertEquals(responseDTO.getBranch().getCommits().get(0).getId(), COMMIT_ID);
  }
}
