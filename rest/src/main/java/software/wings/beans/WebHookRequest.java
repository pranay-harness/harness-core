package software.wings.beans;

public class WebHookRequest {
  private String buildNo;
  private String imageTag;

  public String getBuildNo() {
    return buildNo;
  }

  public void setBuildNo(String buildNo) {
    this.buildNo = buildNo;
  }

  public String getImageTag() {
    return imageTag;
  }

  public void setImageTag(String imageTag) {
    this.imageTag = imageTag;
  }
}
