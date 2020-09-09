package io.harness.app.impl;

import io.harness.app.beans.dto.CIBuildResponseDTO;
import io.harness.app.beans.entities.CIBuildPipeline;
import io.harness.ci.beans.entities.CIBuild;

public class CIBuildInfoServiceImplTestHelper {
  public static final String ACCOUNT_ID = "ACCOUNT_ID";
  public static final String ORG_ID = "ORG_ID";
  public static final String PROJECT_ID = "PROJECT_ID";
  public static final Long BUILD_ID = 4L;
  public static final String PIPELINE_ID = "123";

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
}
