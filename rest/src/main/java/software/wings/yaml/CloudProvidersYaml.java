package software.wings.yaml;

import java.util.ArrayList;
import java.util.List;

public class CloudProvidersYaml extends GenericYaml {
  @YamlSerialize public List<String> AWS = new ArrayList<>();
  @YamlSerialize public List<String> googleCloudPlatform = new ArrayList<>();
  @YamlSerialize public List<String> physicalDataCenters = new ArrayList<>();

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
