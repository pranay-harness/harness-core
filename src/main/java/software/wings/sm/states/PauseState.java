package software.wings.sm.states;

import static software.wings.api.PauseStateExecutionData.Builder.aPauseStateExecutionData;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;

import software.wings.api.EmailStateExecutionData;
import software.wings.api.PauseStateExecutionData;
import software.wings.common.UUIDGenerator;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.StateType;
import software.wings.utils.MapperUtils;

// TODO: Auto-generated Javadoc

/**
 * A Pause state to pause state machine execution.
 *
 * @author Rishi
 */
public class PauseState extends EmailState {
  private static final long serialVersionUID = 1L;

  /**
   * Creates pause state with given name.
   *
   * @param name name of the state.
   */
  public PauseState(String name) {
    super(name);
    setStateType(StateType.PAUSE.getType());
  }

  /** {@inheritDoc} */
  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    ExecutionResponse emailExecutionResponse = super.execute(context);

    EmailStateExecutionData emailStateExecutionData =
        (EmailStateExecutionData) emailExecutionResponse.getStateExecutionData();

    String correlationId = UUIDGenerator.getUuid();

    PauseStateExecutionData pauseStateExecutionData = aPauseStateExecutionData().withResumeId(correlationId).build();
    MapperUtils.mapObject(emailStateExecutionData, pauseStateExecutionData);

    return anExecutionResponse()
        .withAsynch(true)
        .addCorrelationIds(correlationId)
        .withStateExecutionData(pauseStateExecutionData)
        .build();
  }
}
