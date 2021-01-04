package io.harness.engine.outputs;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.pms.data.PmsSweepingOutputService;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.refobjects.RefObject;
import io.harness.pms.sdk.core.data.SweepingOutput;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.serializer.persistence.DocumentOrchestrationUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

// TODO (prashant) : Remove this implementation totally when everything is remote

@OwnedBy(CDC)
@Singleton
public class ExecutionSweepingOutputServiceImpl implements ExecutionSweepingOutputService {
  @Inject private PmsSweepingOutputService pmsSweepingOutputService;

  @Override
  public String consume(Ambiance ambiance, String name, SweepingOutput value, String groupName) {
    return pmsSweepingOutputService.consume(
        ambiance, name, DocumentOrchestrationUtils.convertToDocumentJson(value), groupName);
  }

  @Override
  public SweepingOutput resolve(Ambiance ambiance, RefObject refObject) {
    String json = pmsSweepingOutputService.resolve(ambiance, refObject);
    return DocumentOrchestrationUtils.convertFromDocumentJson(json);
  }
}
