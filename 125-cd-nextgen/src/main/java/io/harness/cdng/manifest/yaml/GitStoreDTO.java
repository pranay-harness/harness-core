package io.harness.cdng.manifest.yaml;

import io.harness.delegate.beans.storeconfig.FetchType;
import io.harness.pms.yaml.ParameterField;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GitStoreDTO implements GitStoreConfigDTO {
  String connectorRef;

  FetchType gitFetchType;
  String branch;
  String commitId;

  List<String> paths;
  String folderPath;
  String repoName;

  @Override
  public GitStore toGitStoreConfig() {
    return GitStore.builder()
        .branch(ParameterField.createValueField(branch))
        .commitId(ParameterField.createValueField(commitId))
        .connectorRef(ParameterField.createValueField(connectorRef))
        .folderPath(ParameterField.createValueField(folderPath))
        .gitFetchType(gitFetchType)
        .paths(ParameterField.createValueField(paths))
        .repoName(ParameterField.createValueField(repoName))
        .build();
  }
}
