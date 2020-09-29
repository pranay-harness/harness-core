package io.harness.app.impl;

import io.harness.app.beans.dto.CIBuildFilterDTO;
import io.harness.app.beans.dto.CIBuildResponseDTO;
import io.harness.app.beans.entities.CIBuildPipeline;
import io.harness.cdng.pipeline.CDPipeline;
import io.harness.cdng.pipeline.beans.entities.CDPipelineEntity;
import io.harness.ci.beans.entities.CIBuild;

import java.util.Arrays;

public class CIBuildInfoServiceImplTestHelper {
  public static final String ACCOUNT_ID = "ACCOUNT_ID";
  public static final String ORG_ID = "ORG_ID";
  public static final String PROJECT_ID = "PROJECT_ID";
  public static final Long BUILD_ID = 4L;
  public static final String PIPELINE_ID = "123";
  public static final String PIPELINE_NAME = "test";
  public static final String USER_ID = "foo";
  public static final String BRANCH = "master";
  public static final String TAG = "foo";

  public static CIBuild getBasicBuild() {
    return CIBuild.builder()
        .buildNumber(BUILD_ID)
        .accountIdentifier(ACCOUNT_ID)
        .orgIdentifier(ORG_ID)
        .projectIdentifier(PROJECT_ID)
        .pipelineIdentifier(PIPELINE_ID)
        .build();
  }

  public static CIBuildResponseDTO getBasicBuildDTO() {
    return CIBuildResponseDTO.builder()
        .id(BUILD_ID)
        .pipeline(CIBuildPipeline.builder().id(PIPELINE_ID).build())
        .build();
  }

  public static CDPipelineEntity getPipeline() {
    return CDPipelineEntity.builder()
        .identifier(PIPELINE_ID)
        .cdPipeline(CDPipeline.builder().name(PIPELINE_NAME).build())
        .build();
  }

  public static CIBuildFilterDTO getBuildFilter() {
    return CIBuildFilterDTO.builder()
        .accountIdentifier(ACCOUNT_ID)
        .orgIdentifier(ORG_ID)
        .projectIdentifier(PROJECT_ID)
        .userIdentifier(USER_ID)
        .branch(BRANCH)
        .tags(Arrays.asList(TAG))
        .build();
  }
}
