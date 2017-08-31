package software.wings.yaml;

import java.util.ArrayList;
import java.util.List;

public class YamlVersionList implements YamlHistory {
  List<YamlVersion> versions = new ArrayList<YamlVersion>();

  public YamlVersionList() {}

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
