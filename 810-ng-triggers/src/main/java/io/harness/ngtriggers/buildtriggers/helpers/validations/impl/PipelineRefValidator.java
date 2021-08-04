package io.harness.ngtriggers.buildtriggers.helpers.validations.impl;

import io.harness.exception.InvalidRequestException;
import io.harness.ngtriggers.beans.dto.TriggerDetails;
import io.harness.ngtriggers.buildtriggers.helpers.BuildTriggerHelper;
import io.harness.ngtriggers.buildtriggers.helpers.validations.TriggerValidator;
import io.harness.pms.pipeline.PipelineEntity;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Optional;
import lombok.AllArgsConstructor;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class PipelineRefValidator implements TriggerValidator {
  private final BuildTriggerHelper validationHelper;
  @Override
  public void validate(TriggerDetails triggerDetails) {
    Optional<PipelineEntity> pipelineEntity =
        validationHelper.fetchPipelineForTrigger(triggerDetails.getNgTriggerEntity());

    if (!pipelineEntity.isPresent()) {
      throw new InvalidRequestException("");
    }
  }
}
