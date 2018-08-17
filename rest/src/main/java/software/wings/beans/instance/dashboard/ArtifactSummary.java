package software.wings.beans.instance.dashboard;

/**
 * Artifact information
 * @author rktummala on 08/13/17
 */
public class ArtifactSummary extends AbstractEntitySummary {
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

  public static final class Builder {
    private String id;
    private String name;
    private String type;
    private String artifactSourceName;
    private String buildNo;

    private Builder() {}

    public static Builder anArtifactSummary() {
      return new Builder();
    }

    public Builder id(String id) {
      this.id = id;
      return this;
    }

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder type(String type) {
      this.type = type;
      return this;
    }

    public Builder artifactSourceName(String artifactSourceName) {
      this.artifactSourceName = artifactSourceName;
      return this;
    }

    public Builder buildNo(String buildNo) {
      this.buildNo = buildNo;
      return this;
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
