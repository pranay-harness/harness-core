package software.wings.api;

import org.mongodb.morphia.annotations.Entity;
import software.wings.beans.infrastructure.Instance;
import software.wings.core.queue.Queuable;

import java.util.Date;
import java.util.List;

/**
 * This is a wrapper class of ExecutionContext to make it extend queuable.
 * This is used as input for capturing instance information.
 * @author rktummala on 08/24/17
 *
 */
@Entity(value = "instanceChangeQueue", noClassnameStored = true)
public class InstanceChangeEvent extends Queuable {
  private List<Instance> instanceList;

  public List<Instance> getInstanceList() {
    return instanceList;
  }

  public void setInstanceList(List<Instance> instanceList) {
    this.instanceList = instanceList;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    InstanceChangeEvent that = (InstanceChangeEvent) o;

    return instanceList != null ? instanceList.equals(that.instanceList) : that.instanceList == null;
  }

  @Override
  public int hashCode() {
    return instanceList != null ? instanceList.hashCode() : 0;
  }

  @Override
  public String toString() {
    return "InstanceUpdateEvent{"
        + "instanceList=" + instanceList + '}';
  }

  public static final class Builder {
    private List<Instance> instanceList;
    private String id;
    private boolean running = false;
    private Date resetTimestamp = new Date(Long.MAX_VALUE);
    private Date earliestGet = new Date();
    private double priority = 0.0;
    private Date created = new Date();
    private int retries = 0;

    private Builder() {}

    public static Builder anInstanceUpdateEvent() {
      return new Builder();
    }

    public Builder withInstanceList(List<Instance> instanceList) {
      this.instanceList = instanceList;
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
      return anInstanceUpdateEvent()
          .withInstanceList(instanceList)
          .withId(id)
          .withRunning(running)
          .withResetTimestamp(resetTimestamp)
          .withEarliestGet(earliestGet)
          .withPriority(priority)
          .withCreated(created)
          .withRetries(retries);
    }

    public InstanceChangeEvent build() {
      InstanceChangeEvent instanceUpdateEvent = new InstanceChangeEvent();
      instanceUpdateEvent.setInstanceList(instanceList);
      instanceUpdateEvent.setId(id);
      instanceUpdateEvent.setRunning(running);
      instanceUpdateEvent.setResetTimestamp(resetTimestamp);
      instanceUpdateEvent.setEarliestGet(earliestGet);
      instanceUpdateEvent.setPriority(priority);
      instanceUpdateEvent.setCreated(created);
      instanceUpdateEvent.setRetries(retries);
      return instanceUpdateEvent;
    }
  }
}
