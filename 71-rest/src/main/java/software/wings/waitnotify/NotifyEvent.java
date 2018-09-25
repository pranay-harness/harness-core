package software.wings.waitnotify;

import com.google.common.base.MoreObjects;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.harness.queue.Queuable;
import org.mongodb.morphia.annotations.Entity;

import java.util.Date;
import java.util.List;

/**
 * Created by peeyushaggarwal on 4/13/16.
 */
@Entity(value = "notifyQueue", noClassnameStored = true)
public class NotifyEvent extends Queuable {
  private String waitInstanceId;

  private List<String> correlationIds;

  private boolean error;

  /**
   * Instantiates a new notify event.
   */
  public NotifyEvent() {}

  /**
   * Copy constructor.
   *
   * @param other NotifyEvent to create copy for.
   */
  public NotifyEvent(NotifyEvent other) {
    super(other);
    this.waitInstanceId = other.waitInstanceId;
    this.correlationIds = other.correlationIds;
    this.error = other.error;
  }

  /**
   * Gets wait instance id.
   *
   * @return the wait instance id
   */
  public String getWaitInstanceId() {
    return waitInstanceId;
  }

  /**
   * Sets wait instance id.
   *
   * @param waitInstanceId the wait instance id
   */
  public void setWaitInstanceId(String waitInstanceId) {
    this.waitInstanceId = waitInstanceId;
  }

  /**
   * Gets correlation ids.
   *
   * @return the correlation ids
   */
  public List<String> getCorrelationIds() {
    return correlationIds;
  }

  /**
   * Sets correlation ids.
   *
   * @param correlationIds the correlation ids
   */
  public void setCorrelationIds(List<String> correlationIds) {
    this.correlationIds = correlationIds;
  }

  /**
   * Getter for property 'error'.
   *
   * @return Value for property 'error'.
   */
  public boolean isError() {
    return error;
  }

  /**
   * Setter for property 'error'.
   *
   * @param error Value to set for property 'error'.
   */
  public void setError(boolean error) {
    this.error = error;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("waitInstanceId", waitInstanceId)
        .add("correlationIds", correlationIds)
        .add("error", error)
        .toString();
  }

  public static final class Builder {
    private String waitInstanceId;
    private List<String> correlationIds;
    private boolean error;
    private String id;
    private boolean running;
    private Date resetTimestamp = new Date(Long.MAX_VALUE);
    private Date earliestGet = new Date();
    private double priority;
    private Date created = new Date();
    private int retries;

    private Builder() {}

    public static Builder aNotifyEvent() {
      return new Builder();
    }

    public Builder withWaitInstanceId(String waitInstanceId) {
      this.waitInstanceId = waitInstanceId;
      return this;
    }

    public Builder withCorrelationIds(List<String> correlationIds) {
      this.correlationIds = correlationIds;
      return this;
    }

    public Builder withError(boolean error) {
      this.error = error;
      return this;
    }

    public Builder withId(String id) {
      this.id = id;
      return this;
    }

    public Builder withRunning(boolean running) {
      this.running = running;
      return this;
    }

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public Builder withResetTimestamp(Date resetTimestamp) {
      this.resetTimestamp = resetTimestamp;
      return this;
    }

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public Builder withEarliestGet(Date earliestGet) {
      this.earliestGet = earliestGet;
      return this;
    }

    public Builder withPriority(double priority) {
      this.priority = priority;
      return this;
    }

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public Builder withCreated(Date created) {
      this.created = created;
      return this;
    }

    public Builder withRetries(int retries) {
      this.retries = retries;
      return this;
    }

    public Builder but() {
      return aNotifyEvent()
          .withWaitInstanceId(waitInstanceId)
          .withCorrelationIds(correlationIds)
          .withError(error)
          .withId(id)
          .withRunning(running)
          .withResetTimestamp(resetTimestamp)
          .withEarliestGet(earliestGet)
          .withPriority(priority)
          .withCreated(created)
          .withRetries(retries);
    }

    public NotifyEvent build() {
      NotifyEvent notifyEvent = new NotifyEvent();
      notifyEvent.setWaitInstanceId(waitInstanceId);
      notifyEvent.setCorrelationIds(correlationIds);
      notifyEvent.setError(error);
      notifyEvent.setId(id);
      notifyEvent.setRunning(running);
      notifyEvent.setResetTimestamp(resetTimestamp);
      notifyEvent.setEarliestGet(earliestGet);
      notifyEvent.setPriority(priority);
      notifyEvent.setCreated(created);
      notifyEvent.setRetries(retries);
      return notifyEvent;
    }
  }
}
