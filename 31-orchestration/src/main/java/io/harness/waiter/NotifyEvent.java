package io.harness.waiter;

import com.google.common.base.MoreObjects;

import io.harness.annotation.HarnessEntity;
import io.harness.queue.Queuable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = false)
@Entity(value = "notifyQueue", noClassnameStored = true)
@HarnessEntity(exportable = false)
@FieldNameConstants(innerTypeName = "NotifyEventKeys")
public class NotifyEvent extends Queuable {
  @Indexed(options = @IndexOptions(unique = true)) private String waitInstanceId;
  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add(NotifyEventKeys.waitInstanceId, waitInstanceId).toString();
  }

  public static final class Builder {
    private String waitInstanceId;
    private String id;
    private Date earliestGet = new Date();
    private int retries;

    private Builder() {}

    public static Builder aNotifyEvent() {
      return new Builder();
    }

    public Builder waitInstanceId(String waitInstanceId) {
      this.waitInstanceId = waitInstanceId;
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

    public Builder retries(int retries) {
      this.retries = retries;
      return this;
    }

    public NotifyEvent build() {
      NotifyEvent notifyEvent = new NotifyEvent();
      notifyEvent.setWaitInstanceId(waitInstanceId);
      notifyEvent.setId(id);
      notifyEvent.setEarliestGet(earliestGet);
      notifyEvent.setRetries(retries);
      return notifyEvent;
    }
  }
}
