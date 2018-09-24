package software.wings.sm.states;

import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;

import com.github.reinert.jjschema.Attributes;
import software.wings.api.EmailStateExecutionData;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateType;
import software.wings.stencils.DefaultValue;

/**
 * A Pause state to pause state machine execution.
 *
 * @author Rishi
 */
@Attributes
public class PauseState extends EmailState {
  /**
   * Creates pause state with given name.
   *
   * @param name name of the state.
   */
  public PauseState(String name) {
    super(name);
    setStateType(StateType.PAUSE.getType());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    ExecutionResponse emailExecutionResponse = super.execute(context);

    EmailStateExecutionData emailStateExecutionData =
        (EmailStateExecutionData) emailExecutionResponse.getStateExecutionData();

    return anExecutionResponse()
        .withStateExecutionData(emailStateExecutionData)
        .withExecutionStatus(ExecutionStatus.PAUSED)
        .build();
  }

  @DefaultValue(
      value =
          "${workflow.name} execution paused at manual step: ${displayName}. Please click on the link below to resume :\n ${workflow.url}")
  @Attributes(title = "Body")
  @Override
  public String
  getBody() {
    return super.getBody();
  }

  @Attributes(title = "CC")
  @Override
  public String getCcAddress() {
    return super.getCcAddress();
  }

  @Attributes(title = "To", required = true)
  @Override
  public String getToAddress() {
    return super.getToAddress();
  }

  @DefaultValue(value = "Action Required - ${workflow.name} execution paused at manual step ${currentState}")
  @Attributes(title = "Subject", required = true)
  @Override
  public String getSubject() {
    return super.getSubject();
  }

  @Attributes(title = "Ignore Delivery Failure?")
  @Override
  public Boolean isIgnoreDeliveryFailure() {
    return super.isIgnoreDeliveryFailure();
  }
}
