package software.wings.beans;

import com.google.common.collect.Sets;

import com.fasterxml.jackson.annotation.JsonTypeName;
import org.mongodb.morphia.annotations.Reference;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.utils.ArtifactType;

import java.util.Set;

/**
 * Created by anubhaw on 4/13/16.
 */
@JsonTypeName("HTTP")
public class FileUrlSource extends ArtifactStream {
  private String url;

  /**
   * Instantiates a new file url source.
   */
  public FileUrlSource() {
    super(SourceType.HTTP);
  }

  /**
   * Gets url.
   *
   * @return the url
   */
  public String getUrl() {
    return url;
  }

  /**
   * Sets url.
   *
   * @param url the url
   */
  public void setUrl(String url) {
    this.url = url;
  }

  @Override
  public Set<String> getServiceIds() {
    return Sets.newHashSet();
  }

  /**
   * The Class Builder.
   */
  public static final class Builder {
    private String url;
    private String sourceName;
    private ArtifactType artifactType;

    private Builder() {}

    /**
     * A file url source.
     *
     * @return the builder
     */
    public static Builder aFileUrlSource() {
      return new Builder();
    }

    /**
     * With url.
     *
     * @param url the url
     * @return the builder
     */
    public Builder withUrl(String url) {
      this.url = url;
      return this;
    }

    /**
     * With source name.
     *
     * @param sourceName the source name
     * @return the builder
     */
    public Builder withSourceName(String sourceName) {
      this.sourceName = sourceName;
      return this;
    }

    /**
     * With artifact type.
     *
     * @param artifactType the artifact type
     * @return the builder
     */
    public Builder withArtifactType(ArtifactType artifactType) {
      this.artifactType = artifactType;
      return this;
    }

    /**
     * Builds the.
     *
     * @return a new FileUrlSource object with given fields.
     */
    public FileUrlSource build() {
      FileUrlSource fileUrlSource = new FileUrlSource();
      fileUrlSource.setUrl(url);
      fileUrlSource.setSourceName(sourceName);
      fileUrlSource.setArtifactType(artifactType);
      return fileUrlSource;
    }
  }
}
