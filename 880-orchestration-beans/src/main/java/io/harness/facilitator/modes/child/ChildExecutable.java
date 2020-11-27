package io.harness.facilitator.modes.child;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.ambiance.Ambiance;
import io.harness.state.Step;
import io.harness.state.io.StepInputPackage;
import io.harness.state.io.StepParameters;
import io.harness.state.io.StepResponse;
import io.harness.tasks.ResponseData;

import java.util.Map;

/**
 * Use this interface when you want spawn a child
 *
 * This Node will spawn child and the response is passed to handleChildResponse as {@link
 * io.harness.state.io.StepResponseNotifyData}
 *
 */

@OwnedBy(CDC)
@Redesign
public interface ChildExecutable<T extends StepParameters> extends Step<T> {
  ChildExecutableResponse obtainChild(Ambiance ambiance, T stepParameters, StepInputPackage inputPackage);

  StepResponse handleChildResponse(Ambiance ambiance, T stepParameters, Map<String, ResponseData> responseDataMap);
}
