/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.states;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.execution.PublishedFileArtifact;
import io.harness.beans.steps.outcome.StepArtifacts;
import io.harness.beans.steps.outcome.StepArtifacts.StepArtifactsBuilder;
import io.harness.beans.steps.stepinfo.UploadToGCSStepInfo;
import io.harness.delegate.task.stepstatus.artifact.ArtifactMetadata;
import io.harness.delegate.task.stepstatus.artifact.ArtifactMetadataType;
import io.harness.delegate.task.stepstatus.artifact.FileArtifactMetadata;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.pms.contracts.steps.StepType;

@OwnedBy(HarnessTeam.CI)
public class UploadToGCSStep extends AbstractStepExecutable {
  public static final StepType STEP_TYPE = UploadToGCSStepInfo.STEP_TYPE;

  @Override
  protected StepArtifacts handleArtifact(ArtifactMetadata artifactMetadata, StepElementParameters stepParameters) {
    StepArtifactsBuilder stepArtifactsBuilder = StepArtifacts.builder();
    if (artifactMetadata == null) {
      return stepArtifactsBuilder.build();
    }

    if (artifactMetadata.getType() == ArtifactMetadataType.FILE_ARTIFACT_METADATA) {
      FileArtifactMetadata fileArtifactMetadata = (FileArtifactMetadata) artifactMetadata.getSpec();
      if (fileArtifactMetadata != null && isNotEmpty(fileArtifactMetadata.getFileArtifactDescriptors())) {
        fileArtifactMetadata.getFileArtifactDescriptors().forEach(desc
            -> stepArtifactsBuilder.publishedFileArtifact(PublishedFileArtifact.builder().url(desc.getUrl()).build()));
      }
    }
    return stepArtifactsBuilder.build();
  }
}
