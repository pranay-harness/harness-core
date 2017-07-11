package software.wings.helpers.ext.jenkins;

import com.google.common.base.MoreObjects;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Created by peeyushaggarwal on 5/12/16.
 */
public class BuildDetails {
  private String number;
  private String revision;
  private String description;
  private String artifactPath;
  private Map<String, String> buildParameters = new HashMap<>();

  /**
   * Gets number.
   *

   * @return the number
   */
  public String getNumber() {
    return number;
  }

  /**
   * Sets number.
   *
   * @param number the number
   */
  public void setNumber(String number) {
    this.number = number;
  }

  /**
   * Gets revision.
   *
   * @return the revision
   */
  public String getRevision() {
    return revision;
  }

  /**
   * Sets revision.
   *
   * @param revision the revision
   */
  public void setRevision(String revision) {
    this.revision = revision;
  }

  /**
   * Get Artifact Path
   * @return
   */
  public String getArtifactPath() {
    return artifactPath;
  }

  /**
   * Set File name
   * @param artifactPath
   */
  public void setArtifactPath(String artifactPath) {
    this.artifactPath = artifactPath;
  }

  @Override
  public int hashCode() {
    return Objects.hash(number, revision);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final BuildDetails other = (BuildDetails) obj;
    return Objects.equals(this.number, other.number) && Objects.equals(this.revision, other.revision);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("number", number).add("revision", revision).toString();
  }

  /**
   * Gets description.
   *
   * @return the description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Sets description.
   *
   * @param description the description
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * Gets build parameters.
   *
   * @return the build parameters
   */
  public Map<String, String> getBuildParameters() {
    return buildParameters;
  }

  /**
   * Sets build parameters.
   *
   * @param buildParameters the build parameters
   */
  public void setBuildParameters(Map<String, String> buildParameters) {
    this.buildParameters = buildParameters;
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String number;
    private String revision;
    private String description;
    private Map<String, String> buildParameters = new HashMap<>();
    private String artifactPath;

    private Builder() {}

    /**
     * A build details builder.
     *
     * @return the builder
     */
    public static Builder aBuildDetails() {
      return new Builder();
    }

    /**
     * With number builder.
     *
     * @param number the number
     * @return the builder
     */
    public Builder withNumber(String number) {
      this.number = number;
      return this;
    }

    /**
     * With revision builder.
     *
     * @param revision the revision
     * @return the builder
     */
    public Builder withRevision(String revision) {
      this.revision = revision;
      return this;
    }

    /**
     * With description builder.
     *
     * @param description the description
     * @return the builder
     */
    public Builder withDescription(String description) {
      this.description = description;
      return this;
    }

    /**
     * With build parameters builder.
     *
     * @param buildParameters the build parameters
     * @return the builder
     */
    public Builder withBuildParameters(Map<String, String> buildParameters) {
      this.buildParameters = buildParameters;
      return this;
    }

    /**
     * With artifact path
     */
    public Builder withArtifactPath(String artifactPath) {
      this.artifactPath = artifactPath;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return aBuildDetails()
          .withNumber(number)
          .withRevision(revision)
          .withDescription(description)
          .withArtifactPath(artifactPath)
          .withBuildParameters(buildParameters);
    }

    /**
     * Build build details.
     *
     * @return the build details
     */
    public BuildDetails build() {
      BuildDetails buildDetails = new BuildDetails();
      buildDetails.setNumber(number);
      buildDetails.setRevision(revision);
      buildDetails.setDescription(description);
      buildDetails.setBuildParameters(buildParameters);
      buildDetails.setArtifactPath(artifactPath);
      return buildDetails;
    }
  }
}
