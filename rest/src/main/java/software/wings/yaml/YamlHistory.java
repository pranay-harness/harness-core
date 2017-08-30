package software.wings.yaml;

import java.util.ArrayList;
import java.util.List;

public class YamlHistory {
  List<YamlVersion> versions = new ArrayList<YamlVersion>();

  public YamlHistory() {}

  public List<YamlVersion> getVersions() {
    return versions;
  }

  public void setVersions(List<YamlVersion> versions) {
    this.versions = versions;
  }
}
