package software.wings.service.intfc.yaml.sync;

import software.wings.yaml.gitSync.YamlGitConfig;

import java.util.List;
import java.util.Set;

public interface YamlGitConfigService {
  List<YamlGitConfig> getYamlGitConfigAccessibleToUserWithEntityName(String accountId);

  Set<String> getAppIdsForYamlGitConfig(List<String> yamlGitConfigIds);
}
