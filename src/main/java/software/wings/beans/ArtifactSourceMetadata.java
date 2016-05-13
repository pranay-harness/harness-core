package software.wings.beans;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.Maps;

import java.util.Map;

/**
 * Created by peeyushaggarwal on 5/11/16.
 */
public class ArtifactSourceMetadata {
  private String artifactSourceName;
  private Map<String, String> metadata = Maps.newHashMap();

  public String getArtifactSourceName() {
    return artifactSourceName;
  }

  public void setArtifactSourceName(String artifactSourceName) {
    this.artifactSourceName = artifactSourceName;
  }

  public Map<String, String> getMetadata() {
    return metadata;
  }

  public void setMetadata(Map<String, String> metadata) {
    this.metadata = metadata;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ArtifactSourceMetadata that = (ArtifactSourceMetadata) o;
    return Objects.equal(artifactSourceName, that.artifactSourceName) && Objects.equal(metadata, that.metadata);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(artifactSourceName, metadata);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("artifactSourceName", artifactSourceName)
        .add("metadata", metadata)
        .toString();
  }

  public static final class Builder {
    private String artifactSourceName;
    private Map<String, String> metadata = Maps.newHashMap();

    private Builder() {}

    public static Builder anArtifactSourceMetadata() {
      return new Builder();
    }

    public Builder withArtifactSourceName(String artifactSourceName) {
      this.artifactSourceName = artifactSourceName;
      return this;
    }

    public Builder withMetadata(Map<String, String> metadata) {
      this.metadata = metadata;
      return this;
    }

    public Builder but() {
      return anArtifactSourceMetadata().withArtifactSourceName(artifactSourceName).withMetadata(metadata);
    }

    public ArtifactSourceMetadata build() {
      ArtifactSourceMetadata artifactSourceMetadata = new ArtifactSourceMetadata();
      artifactSourceMetadata.setArtifactSourceName(artifactSourceName);
      artifactSourceMetadata.setMetadata(metadata);
      return artifactSourceMetadata;
    }
  }
}
