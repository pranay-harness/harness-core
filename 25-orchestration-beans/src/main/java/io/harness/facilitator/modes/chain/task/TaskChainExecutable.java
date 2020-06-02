package io.harness.facilitator.modes.chain.task;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.ResponseData;
import io.harness.facilitator.PassThroughData;
import io.harness.state.io.StepParameters;
import io.harness.state.io.StepResponse;
import io.harness.state.io.StepTransput;

import java.util.List;
import java.util.Map;

/**
 * Use this interface when you want to execute multiple tasks in a chain inside a single node
 *
 * Example: We want to perform multiple HttpRequests in a single Node
 * 1. HttpChain: Parameters: [http1Params, http2Params, http3Params,. http4Params]
 * 2. Clone a manifest and apply certain operation on the same [GitCloneTask, K8sTask]
 *
 *
 * Interface Details
 *
 * startChainLink : The execution for this step start with this method. It expects as childNodeId in the response
 * based on which we spawn the child. If you set the chain end flag to true in the response we will straight away call
 * finalize execution else we will call executeNextChild. You can add a {@link PassThroughData} in the response which
 * will be passed on to the next method
 *
 * executeNextChild : This is the the repetitive link which is repetitively called until the chainEnd boolean is set in
 * the response.
 *
 * finalizeExecution : This is where the step concludes and responds with step response.
 *
 */

@OwnedBy(CDC)
@Redesign
public interface TaskChainExecutable {
  TaskChainResponse startChainLink(Ambiance ambiance, StepParameters stepParameters, List<StepTransput> inputs);

  TaskChainResponse executeNextLink(Ambiance ambiance, StepParameters stepParameters, List<StepTransput> inputs,
      PassThroughData passThroughData, Map<String, ResponseData> responseDataMap);

  StepResponse finalizeExecution(Ambiance ambiance, StepParameters stepParameters, PassThroughData passThroughData,
      Map<String, ResponseData> responseDataMap);
}
