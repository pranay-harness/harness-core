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
