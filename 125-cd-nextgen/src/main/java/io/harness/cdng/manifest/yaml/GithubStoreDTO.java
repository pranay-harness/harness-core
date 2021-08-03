package io.harness.cdng.manifest.yaml;

import io.harness.beans.ParameterField;
import io.harness.delegate.beans.storeconfig.FetchType;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GithubStoreDTO implements GitStoreConfigDTO {
  String connectorRef;

  FetchType gitFetchType;
  String branch;
  String commitId;

  List<String> paths;
  String folderPath;
  String repoName;

  @Override
  public GitStoreConfig toGitStoreConfig() {
    return GithubStore.builder()
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
