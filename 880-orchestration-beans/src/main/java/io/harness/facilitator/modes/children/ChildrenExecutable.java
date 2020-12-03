package io.harness.facilitator.modes.children;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.ambiance.Ambiance;
import io.harness.pms.execution.ChildrenExecutableResponse;
import io.harness.pms.sdk.core.steps.Step;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.tasks.ResponseData;

import java.util.Map;

/**
 * Use this Executable when you want to spawn multiple children in one go example fork state.
 *
 * InterfaceDefinition:
 *
 * obtainChildren: This expects a list of nodeIds which will be spawned in one go (in parallel). You can also supply any
 * additional inputs per child.
 *
 * handleChildrenResponse: All the responses from children will be accumulated in the response data map. The keys will
 * be {@link io.harness.state.io.StepResponseNotifyData}
 */

@OwnedBy(CDC)
@Redesign
public interface ChildrenExecutable<T extends StepParameters> extends Step<T> {
  ChildrenExecutableResponse obtainChildren(Ambiance ambiance, T stepParameters, StepInputPackage inputPackage);

  StepResponse handleChildrenResponse(Ambiance ambiance, T stepParameters, Map<String, ResponseData> responseDataMap);
}
