/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cdng.manifest.yaml;

import io.harness.cdng.manifest.yaml.storeConfig.StoreConfig;
import io.harness.delegate.beans.storeconfig.FetchType;
import io.harness.pms.yaml.ParameterField;

import java.util.List;

public interface GitStoreConfig extends StoreConfig {
  ParameterField<String> getConnectorRef();
  FetchType getGitFetchType();
  ParameterField<String> getBranch();
  ParameterField<String> getCommitId();
  ParameterField<List<String>> getPaths();
  ParameterField<String> getFolderPath();
  ParameterField<String> getRepoName();
  GitStoreConfigDTO toGitStoreConfigDTO();
}
