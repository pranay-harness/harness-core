package io.harness.cvng.cdng.entities;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import io.harness.cvng.BuilderFactory;
import io.harness.cvng.builderfactory.DefaultObjectCreator;
import io.harness.cvng.cdng.entities.CVNGStepTask.CVNGStepTaskBuilder;

public class CVNGStepTaskCreator implements DefaultObjectCreator<CVNGStepTaskBuilder> {
  @Override
  public CVNGStepTaskBuilder create(BuilderFactory builderFactory) {
    return CVNGStepTask.builder()
        .accountId(builderFactory.getContext().getAccountId())
        .projectIdentifier(builderFactory.getContext().getProjectIdentifier())
        .orgIdentifier(builderFactory.getContext().getOrgIdentifier())
        .activityId(generateUuid())
        .status(CVNGStepTask.Status.IN_PROGRESS)
        .callbackId(generateUuid());
  }
}
