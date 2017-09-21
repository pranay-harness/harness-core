package software.wings.yaml.gitSync.devexample;

import com.google.inject.AbstractModule;

import software.wings.service.impl.yaml.YamlGitSyncServiceImpl;
import software.wings.service.intfc.yaml.YamlGitSyncService;

public class ConnectWithSshKeyModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(YamlGitSyncService.class).to(YamlGitSyncServiceImpl.class);
  }
}
