/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.service.intfc.yaml.sync;

import software.wings.yaml.gitSync.YamlGitConfig;

import java.util.List;
import java.util.Set;

public interface YamlGitConfigService {
  List<YamlGitConfig> getYamlGitConfigAccessibleToUserWithEntityName(String accountId);

  Set<String> getAppIdsForYamlGitConfig(List<String> yamlGitConfigIds);
}
