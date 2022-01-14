package io.harness.engine.pms.data;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.OutcomeInstance;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.data.StepOutcomeRef;
import io.harness.pms.contracts.refobjects.RefObject;

import java.util.List;
import java.util.Map;
import lombok.NonNull;

@OwnedBy(HarnessTeam.PIPELINE)
public interface PmsOutcomeService extends Resolver {
  List<String> findAllByRuntimeId(String planExecutionId, String runtimeId);

  Map<String, String> findAllOutcomesMapByRuntimeId(String planExecutionId, String runtimeId);

  List<String> fetchOutcomes(List<String> outcomeInstanceIds);

  String fetchOutcome(@NonNull String outcomeInstanceId);

  OptionalOutcome resolveOptional(Ambiance ambiance, RefObject refObject);

  List<OutcomeInstance> fetchOutcomeInstanceByRuntimeId(String runtimeId);

  List<String> cloneForRetryExecution(Ambiance ambiance, String originalNodeExecutionId);

  List<StepOutcomeRef> fetchOutcomeRefs(String nodeExecutionId);
}
