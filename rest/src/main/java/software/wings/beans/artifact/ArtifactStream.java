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
import java.util.Date;
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
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "sourceType")
@Entity(value = "artifactStream")
public abstract class ArtifactStream extends Base {
  @NotEmpty private String sourceName;
  @NotNull private SourceType sourceType;
  @NotEmpty private String settingId;
  @NotEmpty private String jobname;

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
   * @param sourceType the source type
   */
  public ArtifactStream(SourceType sourceType) {
    this.sourceType = sourceType;
  }

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
  public String getArtifactDisplayName(int buildNo) {
    return String.format("%s_%s_%s", jobname, buildNo, dateFormat.format(new Date()));
  }

  public String getSourceName() {
    return sourceName;
  }

  public void setSourceName(String sourceName) {
    this.sourceName = sourceName;
  }

  public SourceType getSourceType() {
    return sourceType;
  }

  public String getSettingId() {
    return settingId;
  }

  public void setSettingId(String settingId) {
    this.settingId = settingId;
  }

  public String getJobname() {
    return jobname;
  }

  public void setJobname(String jobname) {
    this.jobname = jobname;
  }

  public List<ArtifactPathServiceEntry> getArtifactPathServices() {
    return artifactPathServices;
  }

  public void setArtifactPathServices(List<ArtifactPathServiceEntry> artifactPathServices) {
    this.artifactPathServices = artifactPathServices;
  }

  public boolean isAutoDownload() {
    return autoDownload;
  }

  public void setAutoDownload(boolean autoDownload) {
    this.autoDownload = autoDownload;
  }

  public boolean isAutoApproveForProduction() {
    return autoApproveForProduction;
  }

  public void setAutoApproveForProduction(boolean autoApproveForProduction) {
    this.autoApproveForProduction = autoApproveForProduction;
  }

  public List<ArtifactStreamAction> getStreamActions() {
    return streamActions;
  }

  public void setStreamActions(List<ArtifactStreamAction> streamActions) {
    this.streamActions = streamActions;
  }

  public Artifact getLastArtifact() {
    return lastArtifact;
  }

  public void setLastArtifact(Artifact lastArtifact) {
    this.lastArtifact = lastArtifact;
  }

  public static DateFormat getDateFormat() {
    return dateFormat;
  }

  /**
   * The Enum SourceType.
   */
  public enum SourceType {
    /**
     * Jenkins source type.
     */
    JENKINS, /**
              * BambooService source type.
              */
    BAMBOO, /**
             * Nexus source type.
             */
    NEXUS, /**
            * Artifactory source type.
            */
    ARTIFACTORY, /**
                  * Svn source type.
                  */
    SVN, /**
          * Git source type.
          */
    GIT, /**
          * Http source type.
          */
    HTTP, /**
           * File upload source type.
           */
    FILE_UPLOAD
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode()
        + Objects.hash(sourceName, sourceType, settingId, jobname, artifactPathServices, autoDownload,
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
    return Objects.equals(this.sourceName, other.sourceName) && Objects.equals(this.sourceType, other.sourceType)
        && Objects.equals(this.settingId, other.settingId) && Objects.equals(this.jobname, other.jobname)
        && Objects.equals(this.artifactPathServices, other.artifactPathServices)
        && Objects.equals(this.autoDownload, other.autoDownload)
        && Objects.equals(this.autoApproveForProduction, other.autoApproveForProduction)
        && Objects.equals(this.streamActions, other.streamActions)
        && Objects.equals(this.lastArtifact, other.lastArtifact);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("sourceName", sourceName)
        .add("sourceType", sourceType)
        .add("settingId", settingId)
        .add("jobname", jobname)
        .add("artifactPathServices", artifactPathServices)
        .add("autoDownload", autoDownload)
        .add("autoApproveForProduction", autoApproveForProduction)
        .add("streamActions", streamActions)
        .add("lastArtifact", lastArtifact)
        .toString();
  }
}
