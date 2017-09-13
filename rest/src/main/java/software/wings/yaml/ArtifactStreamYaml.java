package software.wings.yaml;

import java.util.ArrayList;
import java.util.List;

public class ArtifactStreamYaml extends GenericYaml {
  @YamlSerialize public String artifactStreamType;
  @YamlSerialize public String sourceName;
  @YamlSerialize public String serviceName;
  @YamlSerialize public List<StreamActionYaml> streamActions = new ArrayList<>();

  public String getArtifactStreamType() {
    return artifactStreamType;
  }

  public void setArtifactStreamType(String artifactStreamType) {
    this.artifactStreamType = artifactStreamType;
  }

  public String getSourceName() {
    return sourceName;
  }

  public void setSourceName(String sourceName) {
    this.sourceName = sourceName;
  }

  public String getServiceName() {
    return serviceName;
  }

  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  public List<StreamActionYaml> getStreamActions() {
    return streamActions;
  }

  public void setStreamActions(List<StreamActionYaml> streamActions) {
    this.streamActions = streamActions;
  }
}
