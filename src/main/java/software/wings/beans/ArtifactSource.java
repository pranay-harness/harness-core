package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

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
}
