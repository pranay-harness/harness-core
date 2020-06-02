package io.harness.facilitator.modes.async;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.ResponseData;
import io.harness.facilitator.modes.Abortable;
import io.harness.state.io.StepParameters;
import io.harness.state.io.StepResponse;
import io.harness.state.io.StepTransput;

import java.util.List;
import java.util.Map;

/**
 * An executable interface for async activities. This is used when you want to do async activities other that spawning a
 * delegate task. If your step acts as a wrapper to spawning a delegate task use {@link
 * io.harness.facilitator.modes.task.TaskExecutable}
 *
 * Interface Signature Details:
 *
 * executeAsync: This responds with set of callback ids upon completion of the job associated with this the handleAsync
 * is called with the job response
 *
 * handleAsyncResponse: This concludes this step and responds with the {@link StepResponse}. The result of the job is
 * supplied as parameter in responseDataMap. The key is the callbackId supplied in method above
 *
 */
@OwnedBy(CDC)
@Redesign
public interface AsyncExecutable extends Abortable<AsyncExecutableResponse> {
  AsyncExecutableResponse executeAsync(Ambiance ambiance, StepParameters stepParameters, List<StepTransput> inputs);

  StepResponse handleAsyncResponse(
      Ambiance ambiance, StepParameters stepParameters, Map<String, ResponseData> responseDataMap);
}
