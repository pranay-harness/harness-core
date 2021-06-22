package io.harness.engine.pms.data;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.refobjects.RefObject;
import io.harness.pms.contracts.refobjects.ResolvedRefInput;
import io.harness.pms.data.OrchestrationRefType;

import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.List;

public class PmsTransputHelper {
  @Inject private PmsSweepingOutputService sweepingOutputService;
  @Inject private PmsOutcomeService outcomeService;

  public List<ResolvedRefInput> resolveInputs(Ambiance ambiance, List<RefObject> refObjects) {
    List<ResolvedRefInput> inputs = new ArrayList<>();
    if (!isEmpty(refObjects)) {
      for (RefObject refObject : refObjects) {
        String resolvedInput = null;
        if (refObject.getRefType().getType().equals(OrchestrationRefType.SWEEPING_OUTPUT)) {
          resolvedInput = sweepingOutputService.resolve(ambiance, refObject);
        } else if (refObject.getRefType().getType().equals(OrchestrationRefType.OUTCOME)) {
          resolvedInput = outcomeService.resolve(ambiance, refObject);
        }
        inputs.add(ResolvedRefInput.newBuilder()
                       .setRefObject(refObject)
                       .setTransput(ByteString.copyFromUtf8(resolvedInput))
                       .build());
      }
    }
    return inputs;
  }
}
