package io.harness.cdng.manifest.yaml;

import io.harness.beans.ParameterField;
import io.harness.cdng.manifest.yaml.storeConfig.StoreConfig;
import io.harness.delegate.beans.storeconfig.FetchType;

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
