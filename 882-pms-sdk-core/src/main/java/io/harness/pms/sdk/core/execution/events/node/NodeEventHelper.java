package io.harness.pms.sdk.core.execution.events.node;

import io.harness.pms.contracts.refobjects.ResolvedRefInput;
import io.harness.pms.sdk.core.data.StepTransput;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepInputPackage.StepInputPackageBuilder;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;

import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
public class NodeEventHelper {
  public static StepInputPackage buildStepInputPackage(List<ResolvedRefInput> resolvedInputs) {
    StepInputPackageBuilder inputPackageBuilder = StepInputPackage.builder();

    resolvedInputs.forEach(in -> {
      StepTransput transput = RecastOrchestrationUtils.fromJson(in.getTransput().toStringUtf8(), StepTransput.class);
      inputPackageBuilder.input(io.harness.pms.sdk.core.steps.io.ResolvedRefInput.builder()
                                    .refObject(in.getRefObject())
                                    .transput(transput)
                                    .build());
    });
    return inputPackageBuilder.build();
  }
}
