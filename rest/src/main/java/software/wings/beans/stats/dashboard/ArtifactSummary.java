package software.wings.beans.stats.dashboard;

/**
 * Artifact information
 * @author rktummala on 08/13/17
 */
public class ArtifactSummary extends EntitySummary {
  private String artifactSourceName;
  private String buildNo;

  public String getArtifactSourceName() {
    return artifactSourceName;
  }

  public void setArtifactSourceName(String artifactSourceName) {
    this.artifactSourceName = artifactSourceName;
  }

  public String getBuildNo() {
    return buildNo;
  }

  public void setBuildNo(String buildNo) {
    this.buildNo = buildNo;
  }

  public static final class Builder extends EntitySummary.Builder {
    private String artifactSourceName;
    private String buildNo;

    private Builder() {
      super();
    }

    public static Builder anArtifactSummary() {
      return new Builder();
    }

    public Builder withArtifactSourceName(String artifactSourceName) {
      this.artifactSourceName = artifactSourceName;
      return this;
    }

    public Builder withBuildNo(String buildNo) {
      this.buildNo = buildNo;
      return this;
    }

    public Builder but() {
      return (Builder) anArtifactSummary()
          .withArtifactSourceName(artifactSourceName)
          .withBuildNo(buildNo)
          .withId(id)
          .withName(name)
          .withType(type);
    }

    public ArtifactSummary build() {
      ArtifactSummary artifactSummary = new ArtifactSummary();
      artifactSummary.setId(id);
      artifactSummary.setName(name);
      artifactSummary.setType(type);
      artifactSummary.setArtifactSourceName(artifactSourceName);
      artifactSummary.setBuildNo(buildNo);
      return artifactSummary;
    }
  }
}
