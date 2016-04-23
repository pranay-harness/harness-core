package software.wings.beans;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;

import java.util.Objects;

/**
 * Application bean class.
 *
 * @author Rishi
 */
@Indexes(
    @Index(fields = { @Field("appId")
                      , @Field("name"), @Field("version") }, options = @IndexOptions(unique = true)))
@Entity(value = "platforms", noClassnameStored = true)
public class PlatformSoftware extends BaseFile {
  private String appId;
  private boolean standard;
  private String version;
  private String description;
  private ArtifactSource source;

  public PlatformSoftware() {}

  public PlatformSoftware(String fileName, String md5) {
    super(fileName, md5);
  }

  public String getAppId() {
    return appId;
  }

  public void setAppId(String appId) {
    this.appId = appId;
  }

  public boolean isStandard() {
    return standard;
  }

  public void setStandard(boolean standard) {
    this.standard = standard;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public ArtifactSource getSource() {
    return source;
  }

  public void setSource(ArtifactSource source) {
    this.source = source;
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode() + Objects.hash(appId, standard, version, description, source);
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
    final PlatformSoftware other = (PlatformSoftware) obj;
    return Objects.equals(this.appId, other.appId) && Objects.equals(this.standard, other.standard)
        && Objects.equals(this.version, other.version) && Objects.equals(this.description, other.description)
        && Objects.equals(this.source, other.source);
  }

  @Override
  public String toString() {
    return "PlatformSoftware{"
        + "appId='" + appId + '\'' + ", standard=" + standard + ", version='" + version + '\'' + ", description='"
        + description + '\'' + ", source=" + source + '}';
  }

  public static final class PlatformSoftwareBuilder {
    private boolean active = true;
    private String uuid;
    private User createdBy;
    private long createdAt;
    private User lastUpdatedBy;
    private long lastUpdatedAt;

    private PlatformSoftwareBuilder() {}

    public static PlatformSoftwareBuilder aPlatformSoftware() {
      return new PlatformSoftwareBuilder();
    }

    public PlatformSoftwareBuilder withActive(boolean active) {
      this.active = active;
      return this;
    }

    public PlatformSoftwareBuilder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public PlatformSoftwareBuilder withCreatedBy(User createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    public PlatformSoftwareBuilder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public PlatformSoftwareBuilder withLastUpdatedBy(User lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    public PlatformSoftwareBuilder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    public PlatformSoftwareBuilder but() {
      return aPlatformSoftware()
          .withActive(active)
          .withUuid(uuid)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt);
    }

    /**
     * builds PlatformSoftware object.
     * @return PlatformSoftware object.
     */
    public PlatformSoftware build() {
      PlatformSoftware platformSoftware = new PlatformSoftware();
      platformSoftware.setActive(active);
      platformSoftware.setUuid(uuid);
      platformSoftware.setCreatedBy(createdBy);
      platformSoftware.setCreatedAt(createdAt);
      platformSoftware.setLastUpdatedBy(lastUpdatedBy);
      platformSoftware.setLastUpdatedAt(lastUpdatedAt);
      return platformSoftware;
    }
  }
}
