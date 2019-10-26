package io.harness.waiter;

import com.google.common.base.MoreObjects;

import io.harness.annotation.HarnessEntity;
import io.harness.queue.Queuable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.mongodb.morphia.annotations.Entity;

import java.util.Date;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
@Entity(value = "notifyQueue", noClassnameStored = true)
@HarnessEntity(exportable = false)
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
    private Date earliestGet = new Date();
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

    public Builder earliestGet(Date earliestGet) {
      this.earliestGet = earliestGet == null ? null : (Date) earliestGet.clone();
      return this;
    }

    public Builder created(Date created) {
      this.created = created == null ? null : (Date) created.clone();
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
          .earliestGet(earliestGet)
          .created(created)
          .retries(retries);
    }

    public NotifyEvent build() {
      NotifyEvent notifyEvent = new NotifyEvent();
      notifyEvent.setWaitInstanceId(waitInstanceId);
      notifyEvent.setCorrelationIds(correlationIds);
      notifyEvent.setError(error);
      notifyEvent.setId(id);
      notifyEvent.setEarliestGet(earliestGet);
      notifyEvent.setCreated(created);
      notifyEvent.setRetries(retries);
      return notifyEvent;
    }
  }
}
