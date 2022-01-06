/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.yaml;

import java.util.ArrayList;
import java.util.List;

public class YamlVersionList implements YamlHistory {
  private static final String NO_YAML_IN_VERSION_LIST = "Call with yamlVersionId (Uuid) to get Yaml.";

  private List<YamlVersion> versions = new ArrayList<>();

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
