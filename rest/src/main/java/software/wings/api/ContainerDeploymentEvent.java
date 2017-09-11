package software.wings.api;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.mongodb.morphia.annotations.Entity;
import software.wings.beans.infrastructure.instance.ContainerDeploymentInfo;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.core.queue.Queuable;

import java.util.Date;
import java.util.List;

/**
 * This is a wrapper class of ContainerDeploymentInfo to make it extend queuable.
 * This is used as request for capturing instance information.
 * @author rktummala on 08/24/17
 *
 */
@Entity(value = "containerDeploymentQueue", noClassnameStored = true)
@Data
@EqualsAndHashCode(callSuper = true)
public class ContainerDeploymentEvent extends Queuable {
  private ContainerDeploymentInfo containerDeploymentInfo;

  public static final class Builder {
    private ContainerDeploymentInfo containerDeploymentInfo;
    private String id;
    private boolean running = false;
    private Date resetTimestamp = new Date(Long.MAX_VALUE);
    private Date earliestGet = new Date();
    private double priority = 0.0;
    private Date created = new Date();
    private int retries = 0;

    private Builder() {}

    public static Builder aContainerDeploymentEvent() {
      return new Builder();
    }

    public Builder withContainerDeploymentInfo(ContainerDeploymentInfo containerDeploymentInfo) {
      this.containerDeploymentInfo = containerDeploymentInfo;
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
      return aContainerDeploymentEvent()
          .withContainerDeploymentInfo(containerDeploymentInfo)
          .withId(id)
          .withRunning(running)
          .withResetTimestamp(resetTimestamp)
          .withEarliestGet(earliestGet)
          .withPriority(priority)
          .withCreated(created)
          .withRetries(retries);
    }

    public ContainerDeploymentEvent build() {
      ContainerDeploymentEvent containerDeploymentEvent = new ContainerDeploymentEvent();
      containerDeploymentEvent.setContainerDeploymentInfo(containerDeploymentInfo);
      containerDeploymentEvent.setId(id);
      containerDeploymentEvent.setRunning(running);
      containerDeploymentEvent.setResetTimestamp(resetTimestamp);
      containerDeploymentEvent.setEarliestGet(earliestGet);
      containerDeploymentEvent.setPriority(priority);
      containerDeploymentEvent.setCreated(created);
      containerDeploymentEvent.setRetries(retries);
      return containerDeploymentEvent;
    }
  }
}
