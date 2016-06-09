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

// TODO: Auto-generated Javadoc

/**
 * Artifact bean class.
 *
 * @author Rishi
 */
@Entity(value = "artifacts", noClassnameStored = true)
//@Artifact.ValidArtifact(groups = Create.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Artifact extends Base {
  @Indexed @Reference(idOnly = true) private Release release;

  @Indexed @NotEmpty(groups = Create.class) private String artifactSourceName;

  private Map<String, String> metadata = Maps.newHashMap();

  @Indexed @NotEmpty private String displayName;

  @Indexed @NotEmpty(groups = Create.class) private String revision;

  private List<ArtifactFile> artifactFiles = Lists.newArrayList();

  @Indexed private Status status;

  /**
   * Gets release.
   *
   * @return the release
   */
  public Release getRelease() {
    return release;
  }

  /**
   * Sets release.
   *
   * @param release the release
   */
  public void setRelease(Release release) {
    this.release = release;
  }

  /**
   * Gets artifact source name.
   *
   * @return the artifact source name
   */
  public String getArtifactSourceName() {
    return artifactSourceName;
  }

  /**
   * Sets artifact source name.
   *
   * @param artifactSourceName the artifact source name
   */
  public void setArtifactSourceName(String artifactSourceName) {
    this.artifactSourceName = artifactSourceName;
  }

  /**
   * Gets metadata.
   *
   * @return the metadata
   */
  public Map<String, String> getMetadata() {
    return metadata;
  }

  /**
   * Sets metadata.
   *
   * @param metadata the metadata
   */
  public void setMetadata(Map<String, String> metadata) {
    this.metadata = metadata;
  }

  /**
   * Gets display name.
   *
   * @return the display name
   */
  public String getDisplayName() {
    return displayName;
  }

  /**
   * Sets display name.
   *
   * @param displayName the display name
   */
  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  /**
   * Gets revision.
   *
   * @return the revision
   */
  public String getRevision() {
    return revision;
  }

  /**
   * Sets revision.
   *
   * @param revision the revision
   */
  public void setRevision(String revision) {
    this.revision = revision;
  }

  /**
   * Gets status.
   *
   * @return the status
   */
  public Status getStatus() {
    return status;
  }

  /**
   * Sets status.
   *
   * @param status the status
   */
  public void setStatus(Status status) {
    this.status = status;
  }

  /**
   * Gets artifact files.
   *
   * @return the artifact files
   */
  public List<ArtifactFile> getArtifactFiles() {
    return artifactFiles;
  }

  /**
   * Sets artifact files.
   *
   * @param artifactFiles the artifact files
   */
  public void setArtifactFiles(List<ArtifactFile> artifactFiles) {
    this.artifactFiles = artifactFiles;
  }

  /**
   * Gets sevices.
   *
   * @return the sevices
   */
  @JsonProperty("services")
  public Set<Service> getSevices() {
    return artifactFiles.stream().flatMap(artifactFile -> artifactFile.getServices().stream()).collect(toSet());
  }

  /**
   * Gets source type.
   *
   * @return the source type
   */
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

  /**
   * {@inheritDoc}
   */
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

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    return Objects.hashCode(
        super.hashCode(), release, artifactSourceName, metadata, displayName, revision, artifactFiles, status);
  }

  /**
   * {@inheritDoc}
   */
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

  /**
   * The Enum Status.
   */
  public enum Status {
    /**
     * New status.
     */
    NEW, /**
          * Running status.
          */
    RUNNING, /**
              * Queued status.
              */
    QUEUED, /**
             * Waiting status.
             */
    WAITING, /**
              * Ready status.
              */
    READY, /**
            * Aborted status.
            */
    ABORTED, /**
              * Failed status.
              */
    FAILED, /**
             * Error status.
             */
    ERROR
  }

  /**
   * Created by peeyushaggarwal on 4/4/16.
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Constraint(validatedBy = ValidArtifact.Validator.class)
  public @interface ValidArtifact {
    /**
     * Message.
     *
     * @return the string
     */
    String
    message() default "bean isNotBlank(bean.getApplication().getUuid()) have id for updating and application id is not same.";

    /**
     * Groups.
     *
     * @return the class[]
     */
    Class<?>[] groups() default {};

    /**
     * Payload.
     *
     * @return the class<? extends payload>[]
     */
    Class<? extends Payload>[] payload() default {};

    /**
     * The Class Validator.
     */
    class Validator implements ConstraintValidator<ValidArtifact, Artifact> {
      /**
       * {@inheritDoc}
       */
      @Override
      public void initialize(final ValidArtifact validateForUpdate) {}

      /**
       * {@inheritDoc}
       */
      @Override
      public boolean isValid(final Artifact bean, final ConstraintValidatorContext constraintValidatorContext) {
        return isNotBlank(bean.getAppId()) && isNotBlank(bean.getRelease().getUuid());
      }
    }
  }

  /**
   * The Class Builder.
   */
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

    /**
     * Do not instantiate Builder.
     */
    private Builder() {}

    /**
     * An artifact.
     *
     * @return the builder
     */
    public static Builder anArtifact() {
      return new Builder();
    }

    /**
     * With release.
     *
     * @param release the release
     * @return the builder
     */
    public Builder withRelease(Release release) {
      this.release = release;
      return this;
    }

    /**
     * With artifact source name.
     *
     * @param artifactSourceName the artifact source name
     * @return the builder
     */
    public Builder withArtifactSourceName(String artifactSourceName) {
      this.artifactSourceName = artifactSourceName;
      return this;
    }

    /**
     * With metadata.
     *
     * @param metadata the metadata
     * @return the builder
     */
    public Builder withMetadata(Map<String, String> metadata) {
      this.metadata = metadata;
      return this;
    }

    /**
     * With display name.
     *
     * @param displayName the display name
     * @return the builder
     */
    public Builder withDisplayName(String displayName) {
      this.displayName = displayName;
      return this;
    }

    /**
     * With revision.
     *
     * @param revision the revision
     * @return the builder
     */
    public Builder withRevision(String revision) {
      this.revision = revision;
      return this;
    }

    /**
     * With artifact files.
     *
     * @param artifactFiles the artifact files
     * @return the builder
     */
    public Builder withArtifactFiles(List<ArtifactFile> artifactFiles) {
      this.artifactFiles = artifactFiles;
      return this;
    }

    /**
     * With status.
     *
     * @param status the status
     * @return the builder
     */
    public Builder withStatus(Status status) {
      this.status = status;
      return this;
    }

    /**
     * With uuid.
     *
     * @param uuid the uuid
     * @return the builder
     */
    public Builder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    /**
     * With app id.
     *
     * @param appId the app id
     * @return the builder
     */
    public Builder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    /**
     * With created by.
     *
     * @param createdBy the created by
     * @return the builder
     */
    public Builder withCreatedBy(User createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    /**
     * With created at.
     *
     * @param createdAt the created at
     * @return the builder
     */
    public Builder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    /**
     * With last updated by.
     *
     * @param lastUpdatedBy the last updated by
     * @return the builder
     */
    public Builder withLastUpdatedBy(User lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    /**
     * With last updated at.
     *
     * @param lastUpdatedAt the last updated at
     * @return the builder
     */
    public Builder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    /**
     * With active.
     *
     * @param active the active
     * @return the builder
     */
    public Builder withActive(boolean active) {
      this.active = active;
      return this;
    }

    /**
     * But.
     *
     * @return the builder
     */
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

    /**
     * Builds the.
     *
     * @return the artifact
     */
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
