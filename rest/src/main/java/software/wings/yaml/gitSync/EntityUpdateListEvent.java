package software.wings.yaml.gitSync;

import com.google.common.base.MoreObjects;

import org.mongodb.morphia.annotations.Entity;
import software.wings.core.queue.Queuable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 *
 * @author bsollish 9/26/17
 *
 */
@Entity(value = "entityUpdateListQueue", noClassnameStored = true)
public class EntityUpdateListEvent extends Queuable {
  private List<EntityUpdateEvent> entityUpdateEvents = new ArrayList<EntityUpdateEvent>();
  private String accountId;
  private List<GitSyncFile> gitSyncFiles;

  public void addEntityUpdateEvent(EntityUpdateEvent entityUpdateEvent) {
    if (entityUpdateEvent != null) {
      this.entityUpdateEvents.add(entityUpdateEvent);
    }
  }

  public void addGitSyncFile(GitSyncFile gitSyncFile) {
    if (gitSyncFile != null) {
      this.gitSyncFiles.add(gitSyncFile);
    }
  }

  public List<EntityUpdateEvent> getEntityUpdateEvents() {
    return entityUpdateEvents;
  }

  public void setEntityUpdateEvents(List<EntityUpdateEvent> entityUpdateEvents) {
    this.entityUpdateEvents = entityUpdateEvents;
  }

  public String getAccountId() {
    return accountId;
  }

  public void setAccountId(String accountId) {
    this.accountId = accountId;
  }

  public List<GitSyncFile> getGitSyncFiles() {
    return gitSyncFiles;
  }

  public void setGitSyncFiles(List<GitSyncFile> gitSyncFiles) {
    this.gitSyncFiles = gitSyncFiles;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this)
      return true;
    if (!(o instanceof EntityUpdateListEvent)) {
      return false;
    }
    EntityUpdateListEvent eule = (EntityUpdateListEvent) o;
    return Objects.equals(accountId, eule.accountId) && Objects.equals(entityUpdateEvents, eule.entityUpdateEvents);
  }

  @Override
  public String toString() {
    MoreObjects.toStringHelper(this);
    return MoreObjects.toStringHelper(this)
        .add("accountId", getAccountId())
        .add("entityUpdateEvents", getEntityUpdateEvents())
        .toString();
  }

  public static final class Builder {
    private List<EntityUpdateEvent> entityUpdateEvents;
    private String accountId;
    private List<GitSyncFile> gitSyncFiles;

    private Builder() {}

    public static EntityUpdateListEvent.Builder anEntityUpdateListEvent() {
      return new EntityUpdateListEvent.Builder();
    }

    public EntityUpdateListEvent.Builder withEntityUpdateEvents(List<EntityUpdateEvent> entityUpdateEvents) {
      this.entityUpdateEvents = entityUpdateEvents;
      return this;
    }

    public EntityUpdateListEvent.Builder withAccountId(String accountId) {
      this.accountId = accountId;
      return this;
    }

    public EntityUpdateListEvent.Builder withGitSyncFiles(List<GitSyncFile> gitSyncFiles) {
      this.gitSyncFiles = gitSyncFiles;
      return this;
    }

    public EntityUpdateListEvent.Builder but() {
      return anEntityUpdateListEvent()
          .withEntityUpdateEvents(entityUpdateEvents)
          .withAccountId(accountId)
          .withGitSyncFiles(gitSyncFiles);
    }

    public EntityUpdateListEvent build() {
      EntityUpdateListEvent entityUpdateListEvent = new EntityUpdateListEvent();
      entityUpdateListEvent.setEntityUpdateEvents(entityUpdateEvents);
      entityUpdateListEvent.setAccountId(accountId);
      entityUpdateListEvent.setGitSyncFiles(gitSyncFiles);
      return entityUpdateListEvent;
    }
  }
}