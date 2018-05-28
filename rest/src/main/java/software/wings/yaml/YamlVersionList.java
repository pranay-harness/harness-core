package software.wings.yaml;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.ArrayList;
import java.util.List;

public class YamlVersionList implements YamlHistory {
  @SuppressFBWarnings("SS_SHOULD_BE_STATIC")
  private final String NO_YAML_IN_VERSION_LIST = "Call with yamlVersionId (Uuid) to get Yaml.";

  List<YamlVersion> versions = new ArrayList<>();

  public YamlVersionList() {}

  public YamlVersionList(List<YamlVersion> versions) {
    for (YamlVersion yv : versions) {
      yv.setYaml(NO_YAML_IN_VERSION_LIST);
      this.versions.add(yv);
    }
  }

  public List<YamlVersion> getVersions() {
    return versions;
  }

  public void setVersions(List<YamlVersion> versions) {
    this.versions = versions;
  }

  public void addVersion(YamlVersion version) {
    this.versions.add(version);
  }
}
