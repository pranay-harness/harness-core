package io.harness.engine.pms.data;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.ExecutionSweepingOutputInstance;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.refobjects.RefObject;

import java.util.List;

@OwnedBy(HarnessTeam.PIPELINE)
public interface PmsSweepingOutputService extends Resolver {
  RawOptionalSweepingOutput resolveOptional(Ambiance ambiance, RefObject refObject);

  List<RawOptionalSweepingOutput> findOutputsUsingNodeId(Ambiance ambiance, String name, List<String> nodeIds);

  List<ExecutionSweepingOutputInstance> fetchOutcomeInstanceByRuntimeId(String runtimeId);

  List<String> cloneForRetryExecution(Ambiance ambiance, String originalNodeExecutionUuid);
}
