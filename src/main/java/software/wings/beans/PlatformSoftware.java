package software.wings.beans;

import org.mongodb.morphia.annotations.*;

import java.util.Objects;

/**
 *  Application bean class.
 *
 *
 * @author Rishi
 *
 */

@Entity(value = "platforms", noClassnameStored = true)
@Indexes(
    @Index(fields = { @Field("appID")
                      , @Field("name"), @Field("version") }, options = @IndexOptions(unique = true)))
public class PlatformSoftware extends BaseFile {
  private String appID;
  private boolean standard;
  private String version;
  private String description;
  private ArtifactSource source;

  public PlatformSoftware() {}

  public PlatformSoftware(String fileName, String md5) {
    super(fileName, md5);
  }

  public String getAppID() {
    return appID;
  }

  public void setAppID(String appID) {
    this.appID = appID;
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
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    if (!super.equals(o))
      return false;
    PlatformSoftware that = (PlatformSoftware) o;
    return standard == that.standard && Objects.equals(appID, that.appID) && Objects.equals(version, that.version)
        && Objects.equals(description, that.description) && Objects.equals(source, that.source);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), appID, standard, version, description, source);
  }

  @Override
  public String toString() {
    return "PlatformSoftware{"
        + "appID='" + appID + '\'' + ", standard=" + standard + ", version='" + version + '\'' + ", description='"
        + description + '\'' + ", source=" + source + '}';
  }
}
