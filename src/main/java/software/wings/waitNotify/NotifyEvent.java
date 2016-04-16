package software.wings.waitNotify;

import com.google.common.base.MoreObjects;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Reference;
import software.wings.core.queue.Queuable;

import java.util.Date;
import java.util.List;

/**
 * Created by peeyushaggarwal on 4/13/16.
 */
@Entity(value = "notifyQueue", noClassnameStored = true)
public class NotifyEvent extends Queuable {
  private String waitInstanceId;

  private List<String> correlationIds;

  public NotifyEvent() {}

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

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("waitInstanceId", waitInstanceId)
        .add("correlationIds", correlationIds)
        .toString();
  }

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

    public Builder withId(String id) {
      this.id = id;
      return this;
    }

    public Builder withRunning(boolean running) {
      this.running = running;
      return this;
    }

    public Builder withResetTimestamp(Date resetTimestamp) {
      this.resetTimestamp = resetTimestamp;
      return this;
    }

    public Builder withEarliestGet(Date earliestGet) {
      this.earliestGet = earliestGet;
      return this;
    }

    public Builder withPriority(double priority) {
      this.priority = priority;
      return this;
    }

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
