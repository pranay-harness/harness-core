package software.wings.collect;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

import io.harness.annotation.HarnessEntity;
import io.harness.queue.Queuable;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Reference;
import software.wings.beans.artifact.Artifact;

import java.util.Date;

@Entity(value = "collectorQueue2", noClassnameStored = true)
@HarnessEntity(exportable = false)
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
    private Date earliestGet = new Date();
    private Date created = new Date();
    private int retries;

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

    public Builder withEarliestGet(Date earliestGet) {
      this.earliestGet = earliestGet == null ? null : (Date) earliestGet.clone();
      return this;
    }

    public Builder withCreated(Date created) {
      this.created = created == null ? null : (Date) created.clone();
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
          .withEarliestGet(earliestGet)
          .withCreated(created)
          .withRetries(retries);
    }

    public CollectEvent build() {
      CollectEvent collectEvent = new CollectEvent();
      collectEvent.setArtifact(artifact);
      collectEvent.setId(id);
      collectEvent.setEarliestGet(earliestGet);
      collectEvent.setCreated(created);
      collectEvent.setRetries(retries);
      return collectEvent;
    }
  }
}
