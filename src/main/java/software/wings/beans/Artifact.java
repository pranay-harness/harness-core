package software.wings.beans;

import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Reference;
import software.wings.beans.ArtifactSource.SourceType;
import software.wings.utils.validation.Create;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;

/**
 * Artifact bean class.
 *
 * @author Rishi
 */
@Entity(value = "artifacts", noClassnameStored = true)
@Artifact.ValidArtifact
@JsonIgnoreProperties(ignoreUnknown = true)
public class Artifact extends Base {
  @Indexed @Reference(idOnly = true) private Release release;

  @Indexed @NotEmpty(groups = Create.class) private String artifactSourceName;

  private Map<String, String> metadata = Maps.newHashMap();

  @Indexed @NotEmpty private String displayName;

  @Indexed @NotEmpty(groups = Create.class) private String revision;

  private List<ArtifactFile> artifactFiles = Lists.newArrayList();

  @Indexed private Status status;

  public Release getRelease() {
    return release;
  }

  public void setRelease(Release release) {
    this.release = release;
  }

  public String getArtifactSourceName() {
    return artifactSourceName;
  }

  public void setArtifactSourceName(String artifactSourceName) {
    this.artifactSourceName = artifactSourceName;
  }

  public Map<String, String> getMetadata() {
    return metadata;
  }

  public void setMetadata(Map<String, String> metadata) {
    this.metadata = metadata;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getRevision() {
    return revision;
  }

  public void setRevision(String revision) {
    this.revision = revision;
  }

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  public List<ArtifactFile> getArtifactFiles() {
    return artifactFiles;
  }

  public void setArtifactFiles(List<ArtifactFile> artifactFiles) {
    this.artifactFiles = artifactFiles;
  }

  @JsonProperty("services")
  public Set<Service> getSevices() {
    return artifactFiles.stream().flatMap(artifactFile -> artifactFile.getServices().stream()).collect(toSet());
  }

  @JsonProperty("sourceType")
  public SourceType getSourceType() {
    if (release != null) {
      Optional<ArtifactSource> artifactSource = release.getArtifactSources()
                                                    .stream()
                                                    .filter(source -> source.getSourceName().equals(artifactSourceName))
                                                    .findFirst();
      if (artifactSource.isPresent()) {
        return artifactSource.get().getSourceType();
      }
    }
    return null;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    if (!super.equals(o))
      return false;
    Artifact artifact = (Artifact) o;
    return Objects.equal(release, artifact.release) && Objects.equal(artifactSourceName, artifact.artifactSourceName)
        && Objects.equal(metadata, artifact.metadata) && Objects.equal(displayName, artifact.displayName)
        && Objects.equal(revision, artifact.revision) && Objects.equal(artifactFiles, artifact.artifactFiles)
        && status == artifact.status;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(
        super.hashCode(), release, artifactSourceName, metadata, displayName, revision, artifactFiles, status);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("release", release)
        .add("artifactSourceName", artifactSourceName)
        .add("metadata", metadata)
        .add("displayName", displayName)
        .add("revision", revision)
        .add("artifactFiles", artifactFiles)
        .add("status", status)
        .add("sevices", getSevices())
        .add("sourceType", getSourceType())
        .toString();
  }

  public enum Status { NEW, RUNNING, QUEUED, WAITING, READY, ABORTED, FAILED, ERROR }

  /**
   * Created by peeyushaggarwal on 4/4/16.
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Constraint(validatedBy = ValidArtifact.Validator.class)
  public static @interface ValidArtifact {
    String
    message() default "bean isNotBlank(bean.getApplication().getUuid()) have id for updating and application id is not same.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    public class Validator implements ConstraintValidator<ValidArtifact, Artifact> {
      @Override
      public void initialize(final ValidArtifact validateForUpdate) {}

      @Override
      public boolean isValid(final Artifact bean, final ConstraintValidatorContext constraintValidatorContext) {
        return isNotBlank(bean.getAppId()) && isNotBlank(bean.getRelease().getUuid());
      }
    }
  }

  public static final class Builder {
    private Release release;
    private String artifactSourceName;
    private Map<String, String> metadata = Maps.newHashMap();
    private String displayName;
    private String revision;
    private List<ArtifactFile> artifactFiles = Lists.newArrayList();
    private Status status;
    private String uuid;
    private String appId;
    private User createdBy;
    private long createdAt;
    private User lastUpdatedBy;
    private long lastUpdatedAt;
    private boolean active = true;

    private Builder() {}

    public static Builder anArtifact() {
      return new Builder();
    }

    public Builder withRelease(Release release) {
      this.release = release;
      return this;
    }

    public Builder withArtifactSourceName(String artifactSourceName) {
      this.artifactSourceName = artifactSourceName;
      return this;
    }

    public Builder withMetadata(Map<String, String> metadata) {
      this.metadata = metadata;
      return this;
    }

    public Builder withDisplayName(String displayName) {
      this.displayName = displayName;
      return this;
    }

    public Builder withRevision(String revision) {
      this.revision = revision;
      return this;
    }

    public Builder withArtifactFiles(List<ArtifactFile> artifactFiles) {
      this.artifactFiles = artifactFiles;
      return this;
    }

    public Builder withStatus(Status status) {
      this.status = status;
      return this;
    }

    public Builder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public Builder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    public Builder withCreatedBy(User createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    public Builder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public Builder withLastUpdatedBy(User lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    public Builder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    public Builder withActive(boolean active) {
      this.active = active;
      return this;
    }

    public Builder but() {
      return anArtifact()
          .withRelease(release)
          .withArtifactSourceName(artifactSourceName)
          .withMetadata(metadata)
          .withDisplayName(displayName)
          .withRevision(revision)
          .withArtifactFiles(artifactFiles)
          .withStatus(status)
          .withUuid(uuid)
          .withAppId(appId)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt)
          .withActive(active);
    }

    public Artifact build() {
      Artifact artifact = new Artifact();
      artifact.setRelease(release);
      artifact.setArtifactSourceName(artifactSourceName);
      artifact.setMetadata(metadata);
      artifact.setDisplayName(displayName);
      artifact.setRevision(revision);
      artifact.setArtifactFiles(artifactFiles);
      artifact.setStatus(status);
      artifact.setUuid(uuid);
      artifact.setAppId(appId);
      artifact.setCreatedBy(createdBy);
      artifact.setCreatedAt(createdAt);
      artifact.setLastUpdatedBy(lastUpdatedBy);
      artifact.setLastUpdatedAt(lastUpdatedAt);
      artifact.setActive(active);
      return artifact;
    }
  }
}
