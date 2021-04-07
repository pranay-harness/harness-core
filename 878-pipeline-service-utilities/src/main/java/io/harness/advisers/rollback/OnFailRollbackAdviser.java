package io.harness.advisers.rollback;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.advisers.CommonAdviserTypes;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.advisers.AdviserResponse;
import io.harness.pms.contracts.advisers.AdviserType;
import io.harness.pms.contracts.execution.failure.FailureInfo;
import io.harness.pms.execution.utils.StatusUtils;
import io.harness.pms.sdk.core.adviser.Adviser;
import io.harness.pms.sdk.core.adviser.AdvisingEvent;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.serializer.KryoSerializer;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.util.Collections;
import javax.validation.constraints.NotNull;

@OwnedBy(HarnessTeam.CDC)
public class OnFailRollbackAdviser implements Adviser {
  public static final AdviserType ADVISER_TYPE =
      AdviserType.newBuilder().setType(CommonAdviserTypes.ON_FAIL_ROLLBACK.name()).build();
  @Inject private KryoSerializer kryoSerializer;
  @Inject ExecutionSweepingOutputService executionSweepingOutputService;

  @Override
  public AdviserResponse onAdviseEvent(AdvisingEvent advisingEvent) {
    OnFailRollbackParameters onFailRollbackParameters = extractParameters(advisingEvent);
    String nextNodeId = onFailRollbackParameters.getStrategyToUuid().get(onFailRollbackParameters.getStrategy());
    executionSweepingOutputService.consume(advisingEvent.getNodeExecution().getAmbiance(),
        YAMLFieldNameConstants.USE_ROLLBACK_STRATEGY, OnFailRollbackOutput.builder().nextNodeId(nextNodeId).build(),
        YAMLFieldNameConstants.PIPELINE_GROUP);
    return null;
  }

  @Override
  public boolean canAdvise(AdvisingEvent advisingEvent) {
    OnFailRollbackParameters parameters = extractParameters(advisingEvent);
    boolean canAdvise = StatusUtils.brokeStatuses().contains(advisingEvent.getToStatus());
    FailureInfo failureInfo = advisingEvent.getNodeExecution().getFailureInfo();
    if (failureInfo != null && !isEmpty(failureInfo.getFailureTypesList())) {
      return canAdvise
          && !Collections.disjoint(parameters.getApplicableFailureTypes(), failureInfo.getFailureTypesList());
    }
    return canAdvise;
  }

  @NotNull
  private OnFailRollbackParameters extractParameters(AdvisingEvent advisingEvent) {
    return (OnFailRollbackParameters) Preconditions.checkNotNull(
        kryoSerializer.asObject(advisingEvent.getAdviserParameters()));
  }
}
