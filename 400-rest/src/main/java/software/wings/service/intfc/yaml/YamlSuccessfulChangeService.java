/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.service.intfc.yaml;

import software.wings.beans.yaml.GitFileChange;
import software.wings.beans.yaml.YamlSuccessfulChange;
import software.wings.yaml.gitSync.YamlChangeSet;

public interface YamlSuccessfulChangeService {
  String upsert(YamlSuccessfulChange yamlSuccessfulChange);

  void updateOnHarnessChangeSet(YamlChangeSet savedYamlChangeset);

  void updateOnSuccessfulGitChangeProcessing(GitFileChange gitFileChange, String accountId);

  YamlSuccessfulChange get(String accountId, String filePath);
}
