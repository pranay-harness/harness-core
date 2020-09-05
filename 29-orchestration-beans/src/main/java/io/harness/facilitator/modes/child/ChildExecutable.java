package io.harness.facilitator.modes.child;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.state.io.StepInputPackage;
import io.harness.state.io.StepParameters;
import io.harness.state.io.StepResponse;

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
public interface ChildExecutable<T extends StepParameters> {
  ChildExecutableResponse obtainChild(Ambiance ambiance, T stepParameters, StepInputPackage inputPackage);

  StepResponse handleChildResponse(
      Ambiance ambiance, T stepParameters, Map<String, DelegateResponseData> responseDataMap);
}
