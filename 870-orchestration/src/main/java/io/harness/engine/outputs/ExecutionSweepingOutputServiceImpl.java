package io.harness.engine.outputs;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.pms.data.PmsSweepingOutputService;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.refobjects.RefObject;
import io.harness.pms.sdk.core.data.OptionalSweepingOutput;
import io.harness.pms.sdk.core.data.SweepingOutput;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

// TODO (prashant) : Remove this implementation totally when everything is remote

@OwnedBy(CDC)
@Singleton
public class ExecutionSweepingOutputServiceImpl implements ExecutionSweepingOutputService {
  @Inject private PmsSweepingOutputService pmsSweepingOutputService;

  @Override
  public OptionalSweepingOutput resolveOptional(Ambiance ambiance, RefObject refObject) {
    return pmsSweepingOutputService.resolveOptional(ambiance, refObject);
  }

  @Override
  public String consume(Ambiance ambiance, String name, SweepingOutput value, String groupName) {
    return pmsSweepingOutputService.consume(ambiance, name, RecastOrchestrationUtils.toDocumentJson(value), groupName);
  }

  @Override
  public SweepingOutput resolve(Ambiance ambiance, RefObject refObject) {
    String json = pmsSweepingOutputService.resolve(ambiance, refObject);
    return RecastOrchestrationUtils.fromDocumentJson(json, SweepingOutput.class);
  }
}
