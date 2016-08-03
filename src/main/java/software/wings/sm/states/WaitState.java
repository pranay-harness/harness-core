package software.wings.sm.states;

import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;
import static software.wings.sm.ExecutionStatus.ExecutionStatusData.Builder.anExecutionStatusData;

import com.google.inject.name.Named;

import com.github.reinert.jjschema.Attributes;
import org.mongodb.morphia.annotations.Transient;
import software.wings.api.WaitStateExecutionData;
import software.wings.common.UUIDGenerator;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.State;
import software.wings.sm.StateType;
import software.wings.waitnotify.WaitNotifyEngine;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;

/**
 * dummy implementation for wait state.
 *
 * @author Rishi
 */
public class WaitState extends State {
  @Inject @Named("waitStateResumer") @Transient private ScheduledExecutorService executorService;

  @Transient @Inject private WaitNotifyEngine waitNotifyEngine;

  @Attributes(title = "Wait Duration") private long duration;

  /**
   * Creates a new wait state.
   *
   * @param name state name
   */
  public WaitState(String name) {
    super(name, StateType.WAIT.name());
  }

  /* (non-Javadoc)
   * @see software.wings.sm.State#execute(software.wings.sm.ExecutionContext)
   */
  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    WaitStateExecutionData waitStateExecutionData = new WaitStateExecutionData();
    waitStateExecutionData.setDuration(duration);
    long wakeupTs = System.currentTimeMillis() + (duration * 1000);
    waitStateExecutionData.setWakeupTs(wakeupTs);
    waitStateExecutionData.setResumeId(UUIDGenerator.getUuid());

    executorService.schedule(new SimpleNotifier(waitNotifyEngine, waitStateExecutionData.getResumeId(),
                                 anExecutionStatusData().withExecutionStatus(ExecutionStatus.SUCCESS).build()),
        duration, TimeUnit.SECONDS);
    return anExecutionResponse()
        .withAsync(true)
        .addCorrelationIds(waitStateExecutionData.getResumeId())
        .withStateExecutionData(waitStateExecutionData)
        .build();
  }

  /**
   * Handle abort event.
   *
   * @param context the context
   */
  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  /**
   * Gets duration.
   *
   * @return the duration
   */
  public long getDuration() {
    return duration;
  }

  /**
   * Sets duration.
   *
   * @param duration the duration
   */
  public void setDuration(long duration) {
    this.duration = duration;
  }

  public static final class Builder {
    private long duration;
    private String name;

    private Builder() {}

    public static Builder aWaitState() {
      return new Builder();
    }

    public Builder withDuration(long duration) {
      this.duration = duration;
      return this;
    }

    public Builder withName(String name) {
      this.name = name;
      return this;
    }

    public WaitState build() {
      WaitState waitState = new WaitState(name);
      waitState.setDuration(duration);
      return waitState;
    }
  }
}
