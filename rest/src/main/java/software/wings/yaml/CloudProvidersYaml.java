package software.wings.yaml;

import java.util.ArrayList;
import java.util.List;

public class CloudProvidersYaml extends BaseYaml {
  private List<String> AWS = new ArrayList<>();
  private List<String> googleCloudPlatform = new ArrayList<>();
  private List<String> physicalDataCenters = new ArrayList<>();

  public List<String> getAWS() {
    return AWS;
  }

  public void setAWS(List<String> AWS) {
    this.AWS = AWS;
  }

  public List<String> getGoogleCloudPlatform() {
    return googleCloudPlatform;
  }

  public void setGoogleCloudPlatform(List<String> googleCloudPlatform) {
    this.googleCloudPlatform = googleCloudPlatform;
  }

  public List<String> getPhysicalDataCenters() {
    return physicalDataCenters;
  }

  public void setPhysicalDataCenters(List<String> physicalDataCenters) {
    this.physicalDataCenters = physicalDataCenters;
  }
}
