package io.harness.pms.plan.creation.validator;

import static java.lang.String.format;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.contracts.plan.PlanCreationBlobResponse;
import io.harness.pms.contracts.plan.PlanNodeProto;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.pipeline.service.PipelineEnforcementService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import java.util.stream.Collectors;

@OwnedBy(HarnessTeam.PIPELINE)
@Singleton
public class PlanCreationValidator implements CreationValidator<PlanCreationBlobResponse> {
  @Inject PipelineEnforcementService pipelineEnforcementService;

  @Override
  public void validate(String accountId, PlanCreationBlobResponse finalResponse) {
    if (EmptyPredicate.isNotEmpty(finalResponse.getDeps().getDependenciesMap())) {
      throw new InvalidRequestException(
          format("Unable to interpret nodes: %s", finalResponse.getDeps().getDependenciesMap().keySet().toString()));
    }
    if (EmptyPredicate.isEmpty(finalResponse.getStartingNodeId())) {
      throw new InvalidRequestException("Unable to find out starting node");
    }
    List<StepType> stepTypes =
        finalResponse.getNodesMap().values().stream().map(PlanNodeProto::getStepType).collect(Collectors.toList());
    pipelineEnforcementService.checkStepRestrictionAndThrow(accountId, stepTypes);
  }
}
