/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
