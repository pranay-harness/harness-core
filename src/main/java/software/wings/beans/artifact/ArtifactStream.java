package software.wings.beans.artifact;

import com.google.common.base.MoreObjects;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.Base;
import software.wings.beans.Service;
import software.wings.utils.ArtifactType;

import java.util.List;
import java.util.Set;
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

  private SourceType sourceType;

  @NotNull private ArtifactType artifactType;

  private ArtifactDownloadType downloadType;

  private boolean autoApproveForProduction = false;

  private List<PostArtifactDownloadAction> postDownloadActions;

  @Transient private Artifact lastArtifact;

  /**
   * Instantiates a new lastArtifact source.
   *
   * @param sourceType the source type
   */
  public ArtifactStream(SourceType sourceType) {
    this.sourceType = sourceType;
  }

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
  public SourceType getSourceType() {
    return sourceType;
  }

  /**
   * Gets artifact type.
   *
   * @return the artifact type
   */
  public ArtifactType getArtifactType() {
    return artifactType;
  }

  /**
   * Sets artifact type.
   *
   * @param artifactType the artifact type
   */
  public void setArtifactType(ArtifactType artifactType) {
    this.artifactType = artifactType;
  }

  /**
   * Gets services.
   *
   * @return the services
   */
  public abstract Set<Service> getServices();

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

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    ArtifactStream that = (ArtifactStream) obj;
    return com.google.common.base.Objects.equal(sourceName, that.sourceName) && sourceType == that.sourceType
        && artifactType == that.artifactType;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return com.google.common.base.Objects.hashCode(sourceName, sourceType, artifactType);
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("sourceName", sourceName)
        .add("sourceType", sourceType)
        .add("artifactType", artifactType)
        .toString();
  }

  public ArtifactDownloadType getDownloadType() {
    return downloadType;
  }

  public void setDownloadType(ArtifactDownloadType downloadType) {
    this.downloadType = downloadType;
  }

  public boolean isAutoApproveForProduction() {
    return autoApproveForProduction;
  }

  public void setAutoApproveForProduction(boolean autoApproveForProduction) {
    this.autoApproveForProduction = autoApproveForProduction;
  }

  public List<PostArtifactDownloadAction> getPostDownloadActions() {
    return postDownloadActions;
  }

  public void setPostDownloadActions(List<PostArtifactDownloadAction> postDownloadActions) {
    this.postDownloadActions = postDownloadActions;
  }

  /**
   * The enum Artifact download type.
   */
  public enum ArtifactDownloadType {
    /**
     * Automatic artifact download type.
     */
    AUTOMATIC,
    /**
     * Manual artifact download type.
     */
    MANUAL
  }

  /**
   * The Enum SourceType.
   */
  public enum SourceType {
    /**
     * Jenkins source type.
     */
    JENKINS, /**
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
}
