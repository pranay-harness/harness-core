package io.harness.facilitator.modes.chain.child;

import io.harness.ambiance.Ambiance;
import io.harness.delegate.beans.ResponseData;
import io.harness.facilitator.PassThroughData;
import io.harness.state.io.StepParameters;
import io.harness.state.io.StepResponse;
import io.harness.state.io.StepTransput;

import java.util.List;
import java.util.Map;

/**
 * Use this interface when you want to supply a list of children to the node and expect those children to be executed
 * sequentially.
 *
 * Example: We want to group 4 http calls in a section and want them to be executed sequentially
 *
 * Section: Parameters: [http1Id, http2Id, http3I,. http4Id]
 *    --> Http1
 *    --> Http2
 *    --> Http3
 *    --> Http4
 *
 * Interface Details
 *
 * executeFirstChild : The execution for this state start with this method it expects as childNodeId in the response
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
public interface ChildChainExecutable {
  ChildChainResponse executeFirstChild(Ambiance ambiance, StepParameters stepParameters, List<StepTransput> inputs);

  ChildChainResponse executeNextChild(Ambiance ambiance, StepParameters stepParameters, List<StepTransput> inputs,
      PassThroughData passThroughData, Map<String, ResponseData> responseDataMap);

  StepResponse finalizeExecution(Ambiance ambiance, StepParameters stepParameters, PassThroughData passThroughData,
      Map<String, ResponseData> responseDataMap);
}
