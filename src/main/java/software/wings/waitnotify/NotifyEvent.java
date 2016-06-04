package software.wings.waitnotify;

import com.google.common.base.MoreObjects;

import org.mongodb.morphia.annotations.Entity;
import software.wings.core.queue.Queuable;

import java.util.Date;
import java.util.List;

// TODO: Auto-generated Javadoc

/**
 * Created by peeyushaggarwal on 4/13/16.
 */
@Entity(value = "notifyQueue", noClassnameStored = true)
public class NotifyEvent extends Queuable {
  private String waitInstanceId;

  private List<String> correlationIds;

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
  }

  public String getWaitInstanceId() {
    return waitInstanceId;
  }

  public void setWaitInstanceId(String waitInstanceId) {
    this.waitInstanceId = waitInstanceId;
  }

  public List<String> getCorrelationIds() {
    return correlationIds;
  }

  public void setCorrelationIds(List<String> correlationIds) {
    this.correlationIds = correlationIds;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("waitInstanceId", waitInstanceId)
        .add("correlationIds", correlationIds)
        .toString();
  }

  /**
   * The Class Builder.
   */
  public static class Builder {
    private String waitInstanceId;
    private List<String> correlationIds;
    private String id;
    private boolean running = false;
    private Date resetTimestamp = new Date(Long.MAX_VALUE);
    private Date earliestGet = new Date();
    private double priority = 0.0;
    private Date created = new Date();
    private int retries = 0;

    private Builder() {}

    /**
     * A notify event.
     *
     * @return the builder
     */
    public static Builder aNotifyEvent() {
      return new Builder();
    }

    /**
     * But.
     *
     * @return copy of Builder object.
     */
    public Builder but() {
      return aNotifyEvent()
          .withWaitInstanceId(waitInstanceId)
          .withCorrelationIds(correlationIds)
          .withId(id)
          .withRunning(running)
          .withResetTimestamp(resetTimestamp)
          .withEarliestGet(earliestGet)
          .withPriority(priority)
          .withCreated(created)
          .withRetries(retries);
    }

    /**
     * With retries.
     *
     * @param retries the retries
     * @return the builder
     */
    public Builder withRetries(int retries) {
      this.retries = retries;
      return this;
    }

    /**
     * With created.
     *
     * @param created the created
     * @return the builder
     */
    public Builder withCreated(Date created) {
      this.created = created;
      return this;
    }

    /**
     * With priority.
     *
     * @param priority the priority
     * @return the builder
     */
    public Builder withPriority(double priority) {
      this.priority = priority;
      return this;
    }

    /**
     * With earliest get.
     *
     * @param earliestGet the earliest get
     * @return the builder
     */
    public Builder withEarliestGet(Date earliestGet) {
      this.earliestGet = earliestGet;
      return this;
    }

    /**
     * With reset timestamp.
     *
     * @param resetTimestamp the reset timestamp
     * @return the builder
     */
    public Builder withResetTimestamp(Date resetTimestamp) {
      this.resetTimestamp = resetTimestamp;
      return this;
    }

    /**
     * With running.
     *
     * @param running the running
     * @return the builder
     */
    public Builder withRunning(boolean running) {
      this.running = running;
      return this;
    }

    /**
     * With id.
     *
     * @param id the id
     * @return the builder
     */
    public Builder withId(String id) {
      this.id = id;
      return this;
    }

    /**
     * With correlation ids.
     *
     * @param correlationIds the correlation ids
     * @return the builder
     */
    public Builder withCorrelationIds(List<String> correlationIds) {
      this.correlationIds = correlationIds;
      return this;
    }

    /**
     * With wait instance id.
     *
     * @param waitInstanceId the wait instance id
     * @return the builder
     */
    public Builder withWaitInstanceId(String waitInstanceId) {
      this.waitInstanceId = waitInstanceId;
      return this;
    }

    /**
     * Builds the.
     *
     * @return new NotifyEvent object.
     */
    public NotifyEvent build() {
      NotifyEvent notifyEvent = new NotifyEvent();
      notifyEvent.setWaitInstanceId(waitInstanceId);
      notifyEvent.setCorrelationIds(correlationIds);
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
