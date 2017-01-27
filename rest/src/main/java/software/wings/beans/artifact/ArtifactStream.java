package software.wings.beans.artifact;

import com.google.common.base.MoreObjects;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import software.wings.beans.Base;
import software.wings.beans.EmbeddedUser;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.validation.constraints.NotNull;

/**
 * ArtifactStream bean class.
 *
 * @author Rishi
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "artifactStreamType")
@Entity(value = "artifactStream")
public abstract class ArtifactStream extends Base {
  @SchemaIgnore private static final DateFormat dateFormat = new SimpleDateFormat("HHMMSS");

  @NotNull @Attributes(title = "Source Type") private String artifactStreamType;

  @NotEmpty @SchemaIgnore private String sourceName;

  @NotEmpty @Attributes(title = "Source Server") private String settingId;

  private String serviceId;

  @Attributes(title = "Automatic Download") private boolean autoDownload = false;

  @Attributes(title = "Auto-approved for Production") private boolean autoApproveForProduction = false;

  @SchemaIgnore private List<ArtifactStreamAction> streamActions = new ArrayList<>();

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
  @SchemaIgnore
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
  @SchemaIgnore
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
   * Gets stream actions.
   *
   * @return the stream actions
   */
  @SchemaIgnore
  public List<ArtifactStreamAction> getStreamActions() {
    return streamActions;
  }

  @SchemaIgnore
  @Override
  public String getAppId() {
    return super.getAppId();
  }

  @SchemaIgnore
  @Override
  public EmbeddedUser getCreatedBy() {
    return super.getCreatedBy();
  }

  @SchemaIgnore
  @Override
  public EmbeddedUser getLastUpdatedBy() {
    return super.getLastUpdatedBy();
  }

  @SchemaIgnore
  @Override
  public long getCreatedAt() {
    return super.getCreatedAt();
  }

  @SchemaIgnore
  @Override
  public long getLastUpdatedAt() {
    return super.getLastUpdatedAt();
  }

  @SchemaIgnore
  @Override
  public String getUuid() {
    return super.getUuid();
  }

  /**
   * Sets stream actions.
   *
   * @param streamActions the stream actions
   */
  public void setStreamActions(List<ArtifactStreamAction> streamActions) {
    this.streamActions = streamActions;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("sourceName", sourceName)
        .add("artifactStreamType", artifactStreamType)
        .add("settingId", settingId)
        .add("autoDownload", autoDownload)
        .add("autoApproveForProduction", autoApproveForProduction)
        .add("streamActions", streamActions)
        .toString();
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode()
        + Objects.hash(
              sourceName, artifactStreamType, settingId, autoDownload, autoApproveForProduction, streamActions);
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
    final ArtifactStream other = (ArtifactStream) obj;
    return Objects.equals(this.sourceName, other.sourceName)
        && Objects.equals(this.artifactStreamType, other.artifactStreamType)
        && Objects.equals(this.settingId, other.settingId) && Objects.equals(this.autoDownload, other.autoDownload)
        && Objects.equals(this.autoApproveForProduction, other.autoApproveForProduction)
        && Objects.equals(this.streamActions, other.streamActions);
  }

  public String getServiceId() {
    return serviceId;
  }

  public void setServiceId(String serviceId) {
    this.serviceId = serviceId;
  }
}
