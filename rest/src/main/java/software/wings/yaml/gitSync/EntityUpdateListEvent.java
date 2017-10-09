package software.wings.yaml.gitSync;

import com.google.common.base.MoreObjects;

import org.mongodb.morphia.annotations.Entity;
import software.wings.core.queue.Queuable;
import software.wings.yaml.gitSync.EntityUpdateEvent.SourceType;

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
  private List<EntityUpdateEvent> entityUpdateEvents;
  private String accountId;
  private List<GitSyncFile> gitSyncFiles;
  private boolean treeSync = false;
  private boolean filterCustomGitSync = false;
  private SourceType sourceType;

  public void addEntityUpdateEvent(EntityUpdateEvent entityUpdateEvent) {
    if (entityUpdateEvent != null) {
      if (this.entityUpdateEvents == null) {
        this.entityUpdateEvents = new ArrayList<EntityUpdateEvent>();
      }

      this.entityUpdateEvents.add(entityUpdateEvent);
    }
  }

  public void addGitSyncFile(GitSyncFile gitSyncFile) {
    if (gitSyncFile != null) {
      if (this.gitSyncFiles == null) {
        this.gitSyncFiles = new ArrayList<GitSyncFile>();
      }

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

  public boolean isTreeSync() {
    return treeSync;
  }

  public void setTreeSync(boolean treeSync) {
    this.treeSync = treeSync;
  }

  public boolean isFilterCustomGitSync() {
    return filterCustomGitSync;
  }

  public void setFilterCustomGitSync(boolean filterCustomGitSync) {
    this.filterCustomGitSync = filterCustomGitSync;
  }

  public SourceType getSourceType() {
    return sourceType;
  }

  public void setSourceType(SourceType sourceType) {
    this.sourceType = sourceType;
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
        .add("treeSync", isTreeSync())
        .add("filterCustomGitSync", isFilterCustomGitSync())
        .add("sourceType", getSourceType())
        .toString();
  }

  public static final class Builder {
    private List<EntityUpdateEvent> entityUpdateEvents;
    private String accountId;
    private List<GitSyncFile> gitSyncFiles;
    private boolean treeSync;
    private boolean filterCustomGitSync;
    private SourceType sourceType;

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

    public EntityUpdateListEvent.Builder withTreeSync(boolean treeSync) {
      this.treeSync = treeSync;
      return this;
    }

    public EntityUpdateListEvent.Builder withFilterCustomGitSync(boolean filterCustomGitSync) {
      this.filterCustomGitSync = filterCustomGitSync;
      return this;
    }

    public EntityUpdateListEvent.Builder withSourceType(SourceType sourceType) {
      this.sourceType = sourceType;
      return this;
    }

    public EntityUpdateListEvent.Builder but() {
      return anEntityUpdateListEvent()
          .withEntityUpdateEvents(entityUpdateEvents)
          .withAccountId(accountId)
          .withGitSyncFiles(gitSyncFiles)
          .withTreeSync(treeSync)
          .withFilterCustomGitSync(filterCustomGitSync)
          .withSourceType(sourceType);
    }

    public EntityUpdateListEvent build() {
      EntityUpdateListEvent entityUpdateListEvent = new EntityUpdateListEvent();
      entityUpdateListEvent.setEntityUpdateEvents(entityUpdateEvents);
      entityUpdateListEvent.setAccountId(accountId);
      entityUpdateListEvent.setGitSyncFiles(gitSyncFiles);
      entityUpdateListEvent.setTreeSync(treeSync);
      entityUpdateListEvent.setFilterCustomGitSync(filterCustomGitSync);
      entityUpdateListEvent.setSourceType(sourceType);
      return entityUpdateListEvent;
    }
  }
}