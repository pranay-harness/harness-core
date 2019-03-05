package io.harness.waiter;

import com.google.common.base.MoreObjects;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.harness.queue.Queuable;
import lombok.Data;
import org.mongodb.morphia.annotations.Entity;

import java.util.Date;
import java.util.List;

@Data
@Entity(value = "notifyQueue", noClassnameStored = true)
public class NotifyEvent extends Queuable {
  private String waitInstanceId;

  private List<String> correlationIds;

  private boolean error;

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

    public Builder waitInstanceId(String waitInstanceId) {
      this.waitInstanceId = waitInstanceId;
      return this;
    }

    public Builder correlationIds(List<String> correlationIds) {
      this.correlationIds = correlationIds;
      return this;
    }

    public Builder error(boolean error) {
      this.error = error;
      return this;
    }

    public Builder id(String id) {
      this.id = id;
      return this;
    }

    public Builder running(boolean running) {
      this.running = running;
      return this;
    }

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public Builder resetTimestamp(Date resetTimestamp) {
      this.resetTimestamp = resetTimestamp;
      return this;
    }

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public Builder earliestGet(Date earliestGet) {
      this.earliestGet = earliestGet;
      return this;
    }

    public Builder priority(double priority) {
      this.priority = priority;
      return this;
    }

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public Builder created(Date created) {
      this.created = created;
      return this;
    }

    public Builder retries(int retries) {
      this.retries = retries;
      return this;
    }

    public Builder but() {
      return aNotifyEvent()
          .waitInstanceId(waitInstanceId)
          .correlationIds(correlationIds)
          .error(error)
          .id(id)
          .running(running)
          .resetTimestamp(resetTimestamp)
          .earliestGet(earliestGet)
          .priority(priority)
          .created(created)
          .retries(retries);
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
