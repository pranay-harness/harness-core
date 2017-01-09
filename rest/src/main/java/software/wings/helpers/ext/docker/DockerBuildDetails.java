package software.wings.helpers.ext.docker;

import software.wings.helpers.ext.jenkins.BuildDetails;

/**
 * Created by anubhaw on 1/9/17.
 */
public class DockerBuildDetails extends BuildDetails {
  private String tag;

  public DockerBuildDetails(String tag) {
    this.tag = tag;
  }

  public String getTag() {
    return tag;
  }

  public void setTag(String tag) {
    this.tag = tag;
  }
}
