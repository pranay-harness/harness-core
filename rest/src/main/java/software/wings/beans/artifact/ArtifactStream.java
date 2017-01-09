package software.wings.beans.artifact;

import static java.util.stream.Collectors.toSet;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.Base;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * ArtifactStream bean class.
 *
 * @author Rishi
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "artifactStreamType")
@Entity(value = "artifactStream")
public abstract class ArtifactStream extends Base {
  @NotEmpty private String sourceName;
  @NotNull private ArtifactStreamType artifactStreamType;
  @NotEmpty private String settingId;

  @NotEmpty @Valid private List<ArtifactPathServiceEntry> artifactPathServices = Lists.newArrayList();

  private boolean autoDownload = false;

  private boolean autoApproveForProduction = false;

  private List<ArtifactStreamAction> streamActions = new ArrayList<>();

  @Transient private Artifact lastArtifact;

  /**
   * The Date format.
   */
  static final DateFormat dateFormat = new SimpleDateFormat("HHMMSS");

  /**
   * Instantiates a new lastArtifact source.
   *
   * @param artifactStreamType the source type
   */
  public ArtifactStream(ArtifactStreamType artifactStreamType) {
    this.artifactStreamType = artifactStreamType;
  }

  /**
   * Gets service ids.
   *
   * @return the service ids
   */
  public Set<String> getServiceIds() {
    return artifactPathServices.stream()
        .flatMap(artifactPathServiceEntry -> artifactPathServiceEntry.getServiceIds().stream())
        .collect(toSet());
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
  public ArtifactStreamType getArtifactStreamType() {
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
   * Gets artifact path services.
   *
   * @return the artifact path services
   */
  public List<ArtifactPathServiceEntry> getArtifactPathServices() {
    return artifactPathServices;
  }

  /**
   * Sets artifact path services.
   *
   * @param artifactPathServices the artifact path services
   */
  public void setArtifactPathServices(List<ArtifactPathServiceEntry> artifactPathServices) {
    this.artifactPathServices = artifactPathServices;
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
  public List<ArtifactStreamAction> getStreamActions() {
    return streamActions;
  }

  /**
   * Sets stream actions.
   *
   * @param streamActions the stream actions
   */
  public void setStreamActions(List<ArtifactStreamAction> streamActions) {
    this.streamActions = streamActions;
  }

  /**
   * Gets last artifact.
   *
   * @return the last artifact
   */
  public Artifact getLastArtifact() {
    return lastArtifact;
  }

  /**
   * Sets last artifact.
   *
   * @param lastArtifact the last artifact
   */
  public void setLastArtifact(Artifact lastArtifact) {
    this.lastArtifact = lastArtifact;
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
   * The Enum ArtifactStreamType.
   */
  public enum ArtifactStreamType {
    /**
     * Jenkins source type.
     */
    JENKINS, /**
              * BambooService source type.
              */
    BAMBOO, /**
             * Docker source type.
             */
    DOCKER
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("sourceName", sourceName)
        .add("artifactStreamType", artifactStreamType)
        .add("settingId", settingId)
        .add("artifactPathServices", artifactPathServices)
        .add("autoDownload", autoDownload)
        .add("autoApproveForProduction", autoApproveForProduction)
        .add("streamActions", streamActions)
        .add("lastArtifact", lastArtifact)
        .toString();
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode()
        + Objects.hash(sourceName, artifactStreamType, settingId, artifactPathServices, autoDownload,
              autoApproveForProduction, streamActions, lastArtifact);
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
        && Objects.equals(this.settingId, other.settingId)
        && Objects.equals(this.artifactPathServices, other.artifactPathServices)
        && Objects.equals(this.autoDownload, other.autoDownload)
        && Objects.equals(this.autoApproveForProduction, other.autoApproveForProduction)
        && Objects.equals(this.streamActions, other.streamActions)
        && Objects.equals(this.lastArtifact, other.lastArtifact);
  }
}
