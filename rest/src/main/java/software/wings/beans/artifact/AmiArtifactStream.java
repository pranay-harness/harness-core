package software.wings.beans.artifact;

/**
 * Created by sgurubelli on 12/14/17.
 */
public class AmiArtifactStream extends ArtifactStream {
  private String region;
  private String tagName;

  /**
   * AmiArtifactStream
   */
  public AmiArtifactStream() {
    super(ArtifactStreamType.AMI.name());
    super.setAutoApproveForProduction(true);
    super.setAutoDownload(true);
    super.setMetadataOnly(true);
  }

  @Override
  public String getArtifactDisplayName(String amiName) {
    return null;
  }

  @Override
  public String generateName() {
    return null;
  }

  @Override
  public String generateSourceName() {
    return null;
  }

  @Override
  public ArtifactStreamAttributes getArtifactStreamAttributes() {
    return null;
  }

  @Override
  public ArtifactStream clone() {
    return null;
  }

  public String getRegion() {
    return region;
  }

  public void setRegion(String region) {
    this.region = region;
  }

  public String getTagName() {
    return tagName;
  }

  public void setTagName(String tagName) {
    this.tagName = tagName;
  }
}
