package software.wings.yaml.directory;

import software.wings.service.intfc.yaml.YamlGitService;
import software.wings.yaml.YamlVersion;

public class ArtifactStreamYamlNode extends YamlNode {
  private String artifactStreamId;
  private String appId;

  public ArtifactStreamYamlNode() {}

  public ArtifactStreamYamlNode(String accountId, String name, Class theClass) {
    super(accountId, name, theClass);
  }

  public ArtifactStreamYamlNode(String accountId, String appId, String artifactStreamId, String name, Class theClass,
      DirectoryPath directoryPath, YamlGitService yamlGitSyncService, YamlVersion.Type yamlVersionType) {
    super(accountId, artifactStreamId, name, theClass, directoryPath, yamlGitSyncService, yamlVersionType);
    this.artifactStreamId = artifactStreamId;
    this.appId = appId;
  }

  public String getArtifactStreamId() {
    return artifactStreamId;
  }

  public void setArtifactStreamId(String artifactStreamId) {
    this.artifactStreamId = artifactStreamId;
  }

  public String getAppId() {
    return appId;
  }

  public void setAppId(String appId) {
    this.appId = appId;
  }
}
