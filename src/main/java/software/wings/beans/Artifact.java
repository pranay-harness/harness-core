package software.wings.beans;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.common.base.MoreObjects;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Reference;
import software.wings.utils.validation.Create;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;
import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import javax.validation.constraints.NotNull;

/**
 * Artifact bean class.
 *
 * @author Rishi
 */
@Entity(value = "artifacts", noClassnameStored = true)
@Artifact.ValidArtifact
public class Artifact extends Base {
  @Indexed @Reference(idOnly = true) @NotNull private Application application;

  @Indexed @Reference(idOnly = true) @NotNull private Release release;

  @Indexed @NotNull(groups = Create.class) private String compName;

  @Indexed @NotNull(groups = Create.class) private String artifactSourceName;

  @Indexed @NotNull private String displayName;

  @Indexed @NotNull(groups = Create.class) private String revision;

  private ArtifactFile artifactFile;
  @Indexed private Status status;

  public Application getApplication() {
    return application;
  }

  public void setApplication(Application application) {
    this.application = application;
  }

  public Release getRelease() {
    return release;
  }

  public void setRelease(Release release) {
    this.release = release;
  }

  public String getCompName() {
    return compName;
  }

  public void setCompName(String compName) {
    this.compName = compName;
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

  public String getArtifactSourceName() {
    return artifactSourceName;
  }

  public void setArtifactSourceName(String artifactSourceName) {
    this.artifactSourceName = artifactSourceName;
  }

  public ArtifactFile getArtifactFile() {
    return artifactFile;
  }

  public void setArtifactFile(ArtifactFile artifactFile) {
    this.artifactFile = artifactFile;
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode()
        + Objects.hash(application, release, compName, artifactSourceName, displayName, revision, artifactFile, status);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    if (!super.equals(obj)) {
      return false;
    }
    final Artifact other = (Artifact) obj;
    return Objects.equals(this.application, other.application) && Objects.equals(this.release, other.release)
        && Objects.equals(this.compName, other.compName)
        && Objects.equals(this.artifactSourceName, other.artifactSourceName)
        && Objects.equals(this.displayName, other.displayName) && Objects.equals(this.revision, other.revision)
        && Objects.equals(this.artifactFile, other.artifactFile) && Objects.equals(this.status, other.status);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("application", application)
        .add("release", release)
        .add("compName", compName)
        .add("artifactSourceName", artifactSourceName)
        .add("displayName", displayName)
        .add("revision", revision)
        .add("artifactFile", artifactFile)
        .add("status", status)
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
        return bean.getApplication() != null && isNotBlank(bean.getApplication().getUuid())
            && isNotBlank(bean.getRelease().getUuid());
      }
    }
  }

  public static final class ArtifactBuilder {
    private Application application;
    private Release release;
    private String compName;
    private String artifactSourceName;
    private String displayName;
    private String revision;
    private ArtifactFile artifactFile;
    private Status status;
    private String uuid;
    private String appId;
    private User createdBy;
    private long createdAt;
    private User lastUpdatedBy;
    private long lastUpdatedAt;
    private boolean active = true;

    private ArtifactBuilder() {}

    public static ArtifactBuilder anArtifact() {
      return new ArtifactBuilder();
    }

    public ArtifactBuilder withApplication(Application application) {
      this.application = application;
      return this;
    }

    public ArtifactBuilder withRelease(Release release) {
      this.release = release;
      return this;
    }

    public ArtifactBuilder withCompName(String compName) {
      this.compName = compName;
      return this;
    }

    public ArtifactBuilder withArtifactSourceName(String artifactSourceName) {
      this.artifactSourceName = artifactSourceName;
      return this;
    }

    public ArtifactBuilder withDisplayName(String displayName) {
      this.displayName = displayName;
      return this;
    }

    public ArtifactBuilder withRevision(String revision) {
      this.revision = revision;
      return this;
    }

    public ArtifactBuilder withArtifactFile(ArtifactFile artifactFile) {
      this.artifactFile = artifactFile;
      return this;
    }

    public ArtifactBuilder withStatus(Status status) {
      this.status = status;
      return this;
    }

    public ArtifactBuilder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public ArtifactBuilder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    public ArtifactBuilder withCreatedBy(User createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    public ArtifactBuilder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public ArtifactBuilder withLastUpdatedBy(User lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    public ArtifactBuilder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    public ArtifactBuilder withActive(boolean active) {
      this.active = active;
      return this;
    }

    public ArtifactBuilder but() {
      return anArtifact()
          .withApplication(application)
          .withRelease(release)
          .withCompName(compName)
          .withArtifactSourceName(artifactSourceName)
          .withDisplayName(displayName)
          .withRevision(revision)
          .withArtifactFile(artifactFile)
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
      artifact.setApplication(application);
      artifact.setRelease(release);
      artifact.setCompName(compName);
      artifact.setArtifactSourceName(artifactSourceName);
      artifact.setDisplayName(displayName);
      artifact.setRevision(revision);
      artifact.setArtifactFile(artifactFile);
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
