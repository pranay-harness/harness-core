package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.base.MoreObjects;

import java.util.Objects;

/**
 *  ArtifactSource bean class.
 *
 *
 * @author Rishi
 *
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "sourceType")
@JsonSubTypes({ @Type(value = JenkinsArtifactSource.class, name = "JENKINS") })
public abstract class ArtifactSource {
  public enum SourceType { JENKINS, NEXUS, ARTIFACTORY, SVN, GIT, HTTP, FILE_UPLOAD }
  ;

  public enum ArtifactType { JAR, WAR, TAR, ZIP, OTHER }

  public ArtifactSource(SourceType sourceType) {
    this.sourceType = sourceType;
  }

  private String sourceName;
  private SourceType sourceType;
  private ArtifactType artifactType;

  public String getSourceName() {
    return sourceName;
  }

  public void setSourceName(String sourceName) {
    this.sourceName = sourceName;
  }

  public SourceType getSourceType() {
    return sourceType;
  }

  public void setSourceType(SourceType sourceType) {
    this.sourceType = sourceType;
  }

  public ArtifactType getArtifactType() {
    return artifactType;
  }

  public void setArtifactType(ArtifactType artifactType) {
    this.artifactType = artifactType;
  }

  public abstract ArtifactFile collect(Object[] params);

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    ArtifactSource that = (ArtifactSource) o;
    return Objects.equals(sourceName, that.sourceName) && sourceType == that.sourceType
        && artifactType == that.artifactType;
  }

  @Override
  public int hashCode() {
    return Objects.hash(sourceName, sourceType, artifactType);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("sourceName", sourceName)
        .add("sourceType", sourceType)
        .add("artifactType", artifactType)
        .toString();
  }
}
