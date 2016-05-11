package software.wings.helpers.ext;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

import software.wings.beans.Artifact;

import java.util.List;

/**
 * Created by peeyushaggarwal on 5/9/16.
 */
public class Build {
  private String id;
  private String description;
  private BuildStatus buildStatus;
  private List<Artifact> artifacts;
  private String revision;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public BuildStatus getBuildStatus() {
    return buildStatus;
  }

  public void setBuildStatus(BuildStatus buildStatus) {
    this.buildStatus = buildStatus;
  }

  public List<Artifact> getArtifacts() {
    return artifacts;
  }

  public void setArtifacts(List<Artifact> artifacts) {
    this.artifacts = artifacts;
  }

  public String getRevision() {
    return revision;
  }

  public void setRevision(String revision) {
    this.revision = revision;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Build build = (Build) o;
    return Objects.equal(id, build.id) && Objects.equal(description, build.description)
        && buildStatus == build.buildStatus && Objects.equal(artifacts, build.artifacts)
        && Objects.equal(revision, build.revision);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id, description, buildStatus, artifacts, revision);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("id", id)
        .add("description", description)
        .add("buildStatus", buildStatus)
        .add("artifacts", artifacts)
        .add("revision", revision)
        .toString();
  }

  public static final class Builder {
    private String id;
    private String description;
    private BuildStatus buildStatus;
    private List<Artifact> artifacts;
    private String revision;

    private Builder() {}

    public static Builder aBuild() {
      return new Builder();
    }

    public Builder withId(String id) {
      this.id = id;
      return this;
    }

    public Builder withDescription(String description) {
      this.description = description;
      return this;
    }

    public Builder withBuildStatus(BuildStatus buildStatus) {
      this.buildStatus = buildStatus;
      return this;
    }

    public Builder withArtifacts(List<Artifact> artifacts) {
      this.artifacts = artifacts;
      return this;
    }

    public Builder withRevision(String revision) {
      this.revision = revision;
      return this;
    }

    public Build build() {
      Build build = new Build();
      build.setId(id);
      build.setDescription(description);
      build.setBuildStatus(buildStatus);
      build.setArtifacts(artifacts);
      build.setRevision(revision);
      return build;
    }
  }
}
