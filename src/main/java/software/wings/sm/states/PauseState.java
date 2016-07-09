package software.wings.sm.states;

import static software.wings.api.PauseStateExecutionData.Builder.aPauseStateExecutionData;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;
import static software.wings.sm.ExecutionStatus.ExecutionStatusData.Builder.anExecutionStatusData;

import com.github.reinert.jjschema.Attributes;
import org.mongodb.morphia.annotations.Transient;
import software.wings.api.EmailStateExecutionData;
import software.wings.api.PauseStateExecutionData;
import software.wings.beans.WorkflowExecutionEvent;
import software.wings.common.UUIDGenerator;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionEventType;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateType;
import software.wings.utils.MapperUtils;
import software.wings.waitnotify.WaitNotifyEngine;

import javax.inject.Inject;

// TODO: Auto-generated Javadoc

/**
 * A Pause state to pause state machine execution.
 *
 * @author Rishi
 */
@Attributes
public class PauseState extends EmailState {
  @Transient @Inject private WaitNotifyEngine waitNotifyEngine;

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

    String correlationId = UUIDGenerator.getUuid();

    PauseStateExecutionData pauseStateExecutionData = aPauseStateExecutionData().build();
    MapperUtils.mapObject(emailStateExecutionData, pauseStateExecutionData);
    pauseStateExecutionData.setResumeId(correlationId);

    return anExecutionResponse()
        .withAsync(true)
        .addCorrelationIds(correlationId)
        .withStateExecutionData(pauseStateExecutionData)
        .build();
  }

  @Override
  public void handleEvent(ExecutionContextImpl context, WorkflowExecutionEvent workflowExecutionEvent) {
    if (workflowExecutionEvent.getExecutionEventType() == ExecutionEventType.RESUME) {
      PauseStateExecutionData pauseStateExecutionData = (PauseStateExecutionData) context.getStateExecutionData();
      waitNotifyEngine.notify(pauseStateExecutionData.getResumeId(),
          anExecutionStatusData().withExecutionStatus(ExecutionStatus.SUCCESS).build());
      return;
    }
    super.handleEvent(context, workflowExecutionEvent);
  }

  @Attributes(title = "Body")
  @Override
  public String getBody() {
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
