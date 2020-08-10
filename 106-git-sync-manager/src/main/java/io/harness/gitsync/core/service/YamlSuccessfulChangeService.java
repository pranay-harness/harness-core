package io.harness.gitsync.core.service;

import io.harness.gitsync.common.beans.YamlChangeSet;
import io.harness.gitsync.core.beans.YamlSuccessfulChange;

public interface YamlSuccessfulChangeService {
  String upsert(YamlSuccessfulChange yamlSuccessfulChange);

  void updateOnHarnessChangeSet(YamlChangeSet savedYamlChangeset);
}
