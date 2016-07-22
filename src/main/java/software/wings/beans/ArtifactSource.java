package software.wings.beans;

import com.google.common.base.MoreObjects;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Transient;

import java.util.Set;
import javax.validation.constraints.NotNull;

/**
 * ArtifactSource bean class.
 *
 * @author Rishi
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "sourceType")
@JsonSubTypes({
  @Type(value = JenkinsArtifactSource.class, name = "JENKINS")
  , @Type(value = FileUploadSource.class, name = "FILE_UPLOAD"), @Type(value = FileUrlSource.class, name = "HTTP")
})
public abstract class ArtifactSource {
  @NotEmpty private String sourceName;

  @NotNull private SourceType sourceType;

  @NotNull private ArtifactType artifactType;

  @Transient private Artifact lastArtifact;

  /**
   * Instantiates a new lastArtifact source.
   *
   * @param sourceType the source type
   */
  public ArtifactSource(SourceType sourceType) {
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
   * Sets source type.
   *
   * @param sourceType the source type
   */
  public void setSourceType(SourceType sourceType) {
    this.sourceType = sourceType;
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
    ArtifactSource that = (ArtifactSource) obj;
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

  /**
   * The Enum ArtifactType.
   */
  public enum ArtifactType {
    /**
     * Jar artifact type.
     */
    JAR, /**
          * War artifact type.
          */
    WAR, /**
          * Tar artifact type.
          */
    TAR, /**
          * Zip artifact type.
          */
    ZIP, /**
          * Other artifact type.
          */
    OTHER
  }
}
