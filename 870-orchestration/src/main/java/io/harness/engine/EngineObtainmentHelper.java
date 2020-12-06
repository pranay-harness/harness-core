package io.harness.engine;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.exception.InvalidRequestException;
import io.harness.execution.PlanExecution;
import io.harness.plan.Plan;
import io.harness.pms.ambiance.Ambiance;
import io.harness.pms.plan.PlanNodeProto;
import io.harness.pms.refobjects.RefObject;
import io.harness.pms.sdk.core.registries.ResolverRegistry;
import io.harness.pms.sdk.core.resolver.Resolver;
import io.harness.pms.sdk.core.steps.io.ResolvedRefInput;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepInputPackage.StepInputPackageBuilder;

import com.google.inject.Inject;
import java.util.List;

@OwnedBy(CDC)
@Redesign
public class EngineObtainmentHelper {
  @Inject private ResolverRegistry resolverRegistry;
  @Inject private PlanExecutionService planExecutionService;

  public StepInputPackage obtainInputPackage(Ambiance ambiance, List<RefObject> refObjects) {
    StepInputPackageBuilder inputPackageBuilder = StepInputPackage.builder();

    if (!isEmpty(refObjects)) {
      for (RefObject refObject : refObjects) {
        Resolver resolver = resolverRegistry.obtain(refObject.getRefType());
        inputPackageBuilder.input(
            ResolvedRefInput.builder().transput(resolver.resolve(ambiance, refObject)).refObject(refObject).build());
      }
    }
    return inputPackageBuilder.build();
  }

  public PlanNodeProto fetchExecutionNode(String nodeId, String planExecutionId) {
    PlanExecution instance = planExecutionService.get(planExecutionId);
    if (instance == null) {
      throw new InvalidRequestException("Execution Instance is null for id : " + planExecutionId);
    }
    Plan plan = instance.getPlan();
    return plan.fetchNode(nodeId);
  }
}
