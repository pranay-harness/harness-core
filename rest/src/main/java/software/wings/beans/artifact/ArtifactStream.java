package software.wings.beans.artifact;

import com.google.common.base.MoreObjects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;
import software.wings.beans.Base;
import software.wings.yaml.BaseEntityYaml;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * ArtifactStream bean class.
 *
 * @author Rishi
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "artifactStreamType")
@Entity(value = "artifactStream")
@Indexes(
    @Index(fields = { @Field("appId")
                      , @Field("serviceId"), @Field("name") }, options = @IndexOptions(unique = true)))
public abstract class ArtifactStream extends Base {
  private static final DateFormat dateFormat = new SimpleDateFormat("HHMMSS");

  private String artifactStreamType;
  private String sourceName;
  private String settingId;
  private String name;
  private boolean autoPopulate = true;
  @Indexed private String serviceId;
  private boolean autoDownload = true;
  private boolean autoApproveForProduction;
  private boolean metadataOnly;

  /**
   * Instantiates a new lastArtifact source.
   *
   * @param artifactStreamType the source type
   */
  public ArtifactStream(String artifactStreamType) {
    this.artifactStreamType = artifactStreamType;
  }

  /**
   * Gets date format.
   *
   * @return the date format
   */
  public static DateFormat getDateFormat() {
    return dateFormat;
  }

  /**
   * Gets artifact display name.
   *
   * @param buildNo the build no
   * @return the artifact display name
   */
  public abstract String getArtifactDisplayName(String buildNo);

  /**
   * Gets source name.
   *
   * @return the source name
   */
  public String getSourceName() {
    return sourceName;
  }

  /**
   * Sets source name.
   *
   * @param sourceName the source name
   */
  public void setSourceName(String sourceName) {
    this.sourceName = sourceName;
  }

  /**
   * Gets source type.
   *
   * @return the source type
   */
  public String getArtifactStreamType() {
    return artifactStreamType;
  }

  /**
   * Gets setting id.
   *
   * @return the setting id
   */
  public String getSettingId() {
    return settingId;
  }

  /**
   * Sets setting id.
   *
   * @param settingId the setting id
   */
  public void setSettingId(String settingId) {
    this.settingId = settingId;
  }

  /**
   * Is auto download boolean.
   *
   * @return the boolean
   */
  @SchemaIgnore
  public boolean isAutoDownload() {
    return autoDownload;
  }

  /**
   * Sets auto download.
   *
   * @param autoDownload the auto download
   */
  public void setAutoDownload(boolean autoDownload) {
    this.autoDownload = autoDownload;
  }

  /**
   * Is auto approve for production boolean.
   *
   * @return the boolean
   */

  public boolean isAutoApproveForProduction() {
    return autoApproveForProduction;
  }

  /**
   * Sets auto approve for production.
   *
   * @param autoApproveForProduction the auto approve for production
   */
  public void setAutoApproveForProduction(boolean autoApproveForProduction) {
    this.autoApproveForProduction = autoApproveForProduction;
  }

  /**
   * Is metadata only
   * @return
   */

  public boolean isMetadataOnly() {
    return metadataOnly;
  }

  /**
   * Set metadata only
   * @param metadataOnly
   */
  public void setMetadataOnly(boolean metadataOnly) {
    this.metadataOnly = metadataOnly;
  }

  public boolean isAutoPopulate() {
    return autoPopulate;
  }

  public void setAutoPopulate(boolean autoPopulate) {
    this.autoPopulate = autoPopulate;
  }

  public String getName() {
    return name;
  }

  @JsonIgnore public abstract String generateName();

  @JsonIgnore public abstract String generateSourceName();

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("uuid", getUuid())
        .add("serviceId", serviceId)
        .add("sourceName", sourceName)
        .add("artifactStreamType", artifactStreamType)
        .add("settingId", settingId)
        .toString();
  }

  /**
   * Gets service id.
   *
   * @return the service id
   */
  public String getServiceId() {
    return serviceId;
  }

  /**
   * Sets service id.
   *
   * @param serviceId the service id
   */
  public void setServiceId(String serviceId) {
    this.serviceId = serviceId;
  }

  /**
   * Gets artifact stream attributes.
   *
   * @return the artifact stream attributes
   */
  public abstract ArtifactStreamAttributes getArtifactStreamAttributes();

  public abstract ArtifactStream clone();

  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
  public abstract static class Yaml extends BaseEntityYaml {
    private String serverName;
    private boolean metadataOnly;

    public Yaml(String type, String harnessApiVersion, String serverName, boolean metadataOnly) {
      super(type, harnessApiVersion);
      this.serverName = serverName;
      this.metadataOnly = metadataOnly;
    }
  }
}
