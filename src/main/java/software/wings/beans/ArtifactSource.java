package software.wings.beans;

import com.google.common.base.MoreObjects;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.hibernate.validator.constraints.NotEmpty;

import java.util.HashMap;
import java.util.Map;
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

  private Map<String, String> svcAppContainerMap = new HashMap<>();

  public ArtifactSource(SourceType sourceType) {
    this.sourceType = sourceType;
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

  public void setSourceType(SourceType sourceType) {
    this.sourceType = sourceType;
  }

  public ArtifactType getArtifactType() {
    return artifactType;
  }

  public void setArtifactType(ArtifactType artifactType) {
    this.artifactType = artifactType;
  }

  public abstract Set<Service> getServices();

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

  @Override
  public int hashCode() {
    return com.google.common.base.Objects.hashCode(sourceName, sourceType, artifactType);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("sourceName", sourceName)
        .add("sourceType", sourceType)
        .add("artifactType", artifactType)
        .toString();
  }

  public enum SourceType { JENKINS, NEXUS, ARTIFACTORY, SVN, GIT, HTTP, FILE_UPLOAD }

  public enum ArtifactType { JAR, WAR, TAR, ZIP, OTHER }
}
