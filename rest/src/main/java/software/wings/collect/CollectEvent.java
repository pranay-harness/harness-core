package software.wings.collect;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Reference;
import software.wings.beans.artifact.Artifact;
import software.wings.core.queue.Queuable;

import java.util.Date;

/**
 * Created by peeyushaggarwal on 5/11/16.
 */
@Entity(value = "collectorQueue", noClassnameStored = true)
public class CollectEvent extends Queuable {
  @Reference(idOnly = true) private Artifact artifact;

  /**
   * Gets artifact.
   *
   * @return the artifact
   */
  public Artifact getArtifact() {
    return artifact;
  }

  /**
   * Sets artifact.
   *
   * @param artifact the artifact
   */
  public void setArtifact(Artifact artifact) {
    this.artifact = artifact;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
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

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return Objects.hashCode(artifact);
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("artifact", artifact).toString();
  }

  /**
   * The Class Builder.
   */
  public static final class Builder {
    private Artifact artifact;
    private String id;
    private boolean running;
    private Date resetTimestamp = new Date(Long.MAX_VALUE);
    private Date earliestGet = new Date();
    private double priority;
    private Date created = new Date();
    private int retries;

    private Builder() {}

    /**
     * A collect event.
     *
     * @return the builder
     */
    public static Builder aCollectEvent() {
      return new Builder();
    }

    /**
     * With artifact.
     *
     * @param artifact the artifact
     * @return the builder
     */
    public Builder withArtifact(Artifact artifact) {
      this.artifact = artifact;
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
     * With reset timestamp.
     *
     * @param resetTimestamp the reset timestamp
     * @return the builder
     */
    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public Builder withResetTimestamp(Date resetTimestamp) {
      this.resetTimestamp = resetTimestamp;
      return this;
    }

    /**
     * With earliest get.
     *
     * @param earliestGet the earliest get
     * @return the builder
     */
    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public Builder withEarliestGet(Date earliestGet) {
      this.earliestGet = earliestGet;
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
     * With created.
     *
     * @param created the created
     * @return the builder
     */
    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public Builder withCreated(Date created) {
      this.created = created;
      return this;
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
     * But.
     *
     * @return the builder
     */
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

    /**
     * Builds the.
     *
     * @return the collect event
     */
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
