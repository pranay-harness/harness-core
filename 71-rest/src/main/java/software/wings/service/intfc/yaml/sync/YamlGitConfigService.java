package software.wings.service.intfc.yaml.sync;

import software.wings.yaml.gitSync.YamlGitConfig;

import java.util.List;

public interface YamlGitConfigService {
  List<YamlGitConfig> getYamlGitConfigAccessibleToUserWithEntityName(String accountId);
}
