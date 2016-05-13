package software.wings.collect;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Reference;
import software.wings.beans.Artifact;
import software.wings.core.queue.Queuable;

import java.util.Date;

/**
 * Created by peeyushaggarwal on 5/11/16.
 */
@Entity(value = "collectorQueue", noClassnameStored = true)
public class CollectEvent extends Queuable {
  @Reference(idOnly = true) private Artifact artifact;

  public Artifact getArtifact() {
    return artifact;
  }

  public void setArtifact(Artifact artifact) {
    this.artifact = artifact;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CollectEvent that = (CollectEvent) o;
    return Objects.equal(artifact, that.artifact);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(artifact);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("artifact", artifact).toString();
  }

  public static final class Builder {
    private Artifact artifact;
    private String id;
    private boolean running = false;
    private Date resetTimestamp = new Date(Long.MAX_VALUE);
    private Date earliestGet = new Date();
    private double priority = 0.0;
    private Date created = new Date();
    private int retries = 0;

    private Builder() {}

    public static Builder aCollectEvent() {
      return new Builder();
    }

    public Builder withArtifact(Artifact artifact) {
      this.artifact = artifact;
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
      return aCollectEvent()
          .withArtifact(artifact)
          .withId(id)
          .withRunning(running)
          .withResetTimestamp(resetTimestamp)
          .withEarliestGet(earliestGet)
          .withPriority(priority)
          .withCreated(created)
          .withRetries(retries);
    }

    public CollectEvent build() {
      CollectEvent collectEvent = new CollectEvent();
      collectEvent.setArtifact(artifact);
      collectEvent.setId(id);
      collectEvent.setRunning(running);
      collectEvent.setResetTimestamp(resetTimestamp);
      collectEvent.setEarliestGet(earliestGet);
      collectEvent.setPriority(priority);
      collectEvent.setCreated(created);
      collectEvent.setRetries(retries);
      return collectEvent;
    }
  }
}
