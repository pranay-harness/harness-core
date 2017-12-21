package software.wings.sm.states;

import software.wings.sm.ContextElement;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.State;
import software.wings.sm.StateType;

/**
 * Created by anubhaw on 12/19/17.
 */
public class AwsAmiServiceDeployState extends State {
  /**
   * Instantiates a new state.
   *
   * @param name      the name
   */
  public AwsAmiServiceDeployState(String name) {
    super(name, StateType.AWS_AMI_SERVICE_DEPLOY.name());
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    ContextElement contextElement = context.getContextElement(ContextElementType.AMI_SERVICE);

    return ExecutionResponse.Builder.anExecutionResponse()
        .withExecutionStatus(ExecutionStatus.FAILED)
        .withErrorMessage("Not Implemented")
        .build();
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}
}
